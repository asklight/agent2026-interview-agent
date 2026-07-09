#!/usr/bin/env node
const crypto = require('crypto');
const https = require('https');
const fs = require('fs');
const querystring = require('querystring');

const vpnHost = process.env.TJU_VPN_HOST || 'vpn.tju.edu.cn';
const username = process.env.TJU_VPN_USERNAME;
const password = process.env.TJU_VPN_PASSWORD;
const cookieJar = new Map();

function updateCookies(setCookie) {
  if (!setCookie) {
    return;
  }
  for (const raw of setCookie) {
    const first = raw.split(';', 1)[0];
    const index = first.indexOf('=');
    if (index > 0) {
      cookieJar.set(first.slice(0, index), first.slice(index + 1));
    }
  }
}

function cookieHeader() {
  return Array.from(cookieJar.entries())
    .map(([key, value]) => `${key}=${value}`)
    .join('; ');
}

function request(options, body = '', timeoutMs = 30000) {
  return new Promise((resolve, reject) => {
    const req = https.request({
      rejectUnauthorized: false,
      timeout: timeoutMs,
      ...options,
      headers: {
        'User-Agent': 'EasyConnect/7.6.3 Linux',
        ...(options.headers || {}),
      },
    }, (res) => {
      updateCookies(res.headers['set-cookie']);
      let data = '';
      res.setEncoding('utf8');
      res.on('data', (chunk) => {
        data += chunk;
      });
      res.on('end', () => resolve({ statusCode: res.statusCode, headers: res.headers, body: data }));
    });
    req.on('timeout', () => req.destroy(new Error('request_timeout')));
    req.on('error', reject);
    if (body) {
      req.write(body);
    }
    req.end();
  });
}

function tag(xml, name) {
  const pattern = new RegExp(`<${name}>([\\s\\S]*?)</${name}>`, 'i');
  const match = pattern.exec(xml || '');
  return match ? match[1].replace(/^<!\[CDATA\[/, '').replace(/\]\]>$/, '').trim() : '';
}

function rsaEncryptHex(modulusHex, exponentDecimal, text) {
  const modulus = Buffer.from(modulusHex, 'hex');
  const exponent = Buffer.from(Number(exponentDecimal || '65537').toString(16).padStart(6, '0'), 'hex');
  const key = crypto.createPublicKey({ key: rsaSpkiDer(modulus, exponent), format: 'der', type: 'spki' });
  return crypto.publicEncrypt({
    key,
    padding: crypto.constants.RSA_PKCS1_PADDING,
  }, Buffer.from(text, 'utf8')).toString('hex').padStart(modulus.length * 2, '0');
}

function derLength(length) {
  if (length < 128) {
    return Buffer.from([length]);
  }
  const bytes = [];
  let value = length;
  while (value > 0) {
    bytes.unshift(value & 0xff);
    value >>= 8;
  }
  return Buffer.from([0x80 | bytes.length, ...bytes]);
}

function der(tag, value) {
  return Buffer.concat([Buffer.from([tag]), derLength(value.length), value]);
}

function derInteger(value) {
  const normalized = value[0] & 0x80 ? Buffer.concat([Buffer.from([0]), value]) : value;
  return der(0x02, normalized);
}

function rsaSpkiDer(modulus, exponent) {
  const rsaPublicKey = der(0x30, Buffer.concat([derInteger(modulus), derInteger(exponent)]));
  const rsaEncryptionOid = Buffer.from('300d06092a864886f70d0101010500', 'hex');
  const bitString = der(0x03, Buffer.concat([Buffer.from([0]), rsaPublicKey]));
  return der(0x30, Buffer.concat([rsaEncryptionOid, bitString]));
}

async function portalRequest(path, body = '') {
  const headers = {
    Cookie: `${cookieHeader() ? `${cookieHeader()}; ` : ''}allowlogin=1`,
  };
  if (body) {
    headers['Content-Type'] = 'application/x-www-form-urlencoded';
    headers['Content-Length'] = Buffer.byteLength(body);
  }
  return request({
    host: vpnHost,
    port: 443,
    method: 'POST',
    path,
    headers,
  }, body);
}

function sessionHash(token) {
  return crypto.createHash('md5')
    .update(`${token}__md5_salt_for_ecagent_session__`)
    .digest('hex');
}

async function ecAgentCall(token, op, args = {}, timeoutMs = 30000) {
  const params = new URLSearchParams({ op });
  for (const [key, value] of Object.entries(args)) {
    params.append(key, value == null ? '' : String(value));
  }
  params.append('token', sessionHash(token));
  params.append('type', 'EC');

  const response = await request({
    host: '127.0.0.1',
    port: 54530,
    method: 'GET',
    path: `/ECAgent?${params.toString()}`,
  }, '', timeoutMs);

  try {
    return JSON.parse(response.body);
  } catch (error) {
    throw new Error(`ecagent_invalid_json_${op}`);
  }
}

async function waitForCampusApi() {
  for (let i = 0; i < 30; i += 1) {
    try {
      await request({
        host: 'ai.tju.edu.cn',
        port: 443,
        method: 'HEAD',
        path: '/',
      }, '', 5000);
      return;
    } catch (error) {
      await new Promise((resolve) => setTimeout(resolve, 2000));
    }
  }
  throw new Error('campus_api_unreachable_after_vpn_start');
}

async function main() {
  if (!username || !password) {
    throw new Error('vpn_credentials_missing');
  }

  const preAuth = await portalRequest('/por/login_auth.csp?type=cs&dev=linux&encrypt=1&language=zh_CN');
  const rsaKey = tag(preAuth.body, 'RSA_ENCRYPT_KEY');
  const rsaExp = tag(preAuth.body, 'RSA_ENCRYPT_EXP') || '65537';
  if (!rsaKey) {
    throw new Error('vpn_preauth_missing_rsa_key');
  }
  if (tag(preAuth.body, 'RndImg') === '1') {
    throw new Error('vpn_captcha_required');
  }
  if (tag(preAuth.body, 'StartAuth') !== '1') {
    throw new Error('vpn_password_auth_not_first_step');
  }

  const loginBody = querystring.stringify({
    svpn_name: username,
    svpn_password: rsaEncryptHex(rsaKey, rsaExp, password),
  });
  const login = await portalRequest('/por/login_psw.csp?type=cs&dev=linux&language=zh_CN&encrypt=1', loginBody);
  if (tag(login.body, 'Result') !== '1') {
    throw new Error(`vpn_login_failed_${tag(login.body, 'Note') || tag(login.body, 'Message') || 'unknown'}`);
  }

  const token = cookieJar.get('TWFID') || tag(login.body, 'TwfID') || tag(login.body, 'TWFID');
  if (!token) {
    throw new Error('vpn_login_missing_twfid');
  }
  fs.writeFileSync('/tmp/tju-vpn.twfid', token, { mode: 0o600 });

  await ecAgentCall(token, 'InitECAgent', { arg1: `${vpnHost} 443` });
  await ecAgentCall(token, 'DoConfigure', { arg1: `SET SERVADDR ${vpnHost} 443` });
  const encryptKeyResult = await ecAgentCall(token, 'GetEncryptKey');
  if (!encryptKeyResult.result || encryptKeyResult.result === '-100') {
    throw new Error('ecagent_missing_encrypt_key');
  }
  await ecAgentCall(token, 'DoConfigure', {
    arg1: `SET TWFID ${rsaEncryptHex(encryptKeyResult.result, '65537', token)}`,
  });
  await ecAgentCall(token, 'GetConfig', { arg1: '1' }, 61000);
  await ecAgentCall(token, 'GetConfig', { arg1: '2' }, 61000);
  const startResult = await ecAgentCall(token, 'StartService', { arg1: 'EasyConnect' }, 61000);
  if (startResult.result && startResult.result !== '1') {
    throw new Error(`ecagent_start_service_failed_${startResult.result}`);
  }
  await waitForCampusApi();
  console.log('ecvpn ready');
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
