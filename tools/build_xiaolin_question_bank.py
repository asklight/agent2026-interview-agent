#!/usr/bin/env python3
"""Build original cards."""
from __future__ import annotations
import hashlib
import html
import json
import re
import sys
import time
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SITEMAP = "https://www.xiaolincoding.com/sitemap.xml"
PREFIXES = ("/mysql/", "/redis/", "/os/", "/network/", "/backend_interview/")
SOURCE_FILE = ROOT / "docs" / "question-bank-sources.json"
SQL_FILE = ROOT / "apps/server/src/main/resources/data.generated.sql"


def fetch(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": "agent2026-question-bank-builder/1.0"})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8", errors="ignore")


def clean(value: str) -> str:
    value = re.sub(r"<[^>]+>", " ", value)
    value = re.sub(r"\s+", " ", html.unescape(value)).strip()
    return re.sub(r"^#(?:\s*\d+(?:\.\d+)*[.、]?)?\s*", "", value)


def module_for(url: str, heading: str) -> str:
    for prefix, module in (("/mysql/", "MySQL"), ("/redis/", "Redis"), ("/os/", "OperatingSystem"), ("/network/", "Network")):
        if prefix in url:
            return module
    value = heading.lower()
    if any(word in value for word in ("redis", "缓存")):
        return "Redis"
    if any(word in value for word in ("mysql", "sql", "索引", "事务", "数据库")):
        return "MySQL"
    if any(word in value for word in ("tcp", "http", "网络", "dns", "socket")):
        return "Network"
    if any(word in value for word in ("进程", "线程", "内存", "操作系统", "文件系统")):
        return "OperatingSystem"
    if any(word in value for word in ("spring", "bean", "aop", "ioc")):
        return "Spring"
    return "Java"


def discover() -> list[dict[str, str]]:
    sitemap = fetch(SITEMAP)
    urls = sorted(set(html.unescape(value) for value in re.findall(r"<loc>(.*?)</loc>", sitemap)))
    urls = [url for url in urls if any(prefix in url for prefix in PREFIXES) and not url.rstrip("/").endswith("backend_interview")]
    result: dict[tuple[str, str], dict[str, str]] = {}
    for number, url in enumerate(urls, 1):
        try:
            page = fetch(url)
        except OSError:
            continue
        headings = [clean(value) for value in re.findall(r"<h[1-3][^>]*>(.*?)</h[1-3]>", page, re.I | re.S)]
        for heading in dict.fromkeys(item for item in headings if 4 <= len(item) <= 90):
            module = module_for(url, heading)
            result[(module, heading)] = {"url": url, "heading": heading, "module": module}
        print(f"[{number}/{len(urls)}] {url}")
        time.sleep(0.08)
    return sorted(result.values(), key=lambda item: (item["module"], item["heading"]))


def escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace("'", "''")


def render(topic: dict[str, str]) -> str:
    name = topic["heading"].rstrip("？?。.!！")
    digest = hashlib.sha1(f"{topic['module']}|{name}".encode()).hexdigest()[:12]
    difficulty = "hard" if any(word in name.lower() for word in ("原理", "机制", "优化", "排查", "并发", "一致性")) else "medium"
    fields = [
        f"xiaolin-{topic['module'].lower()}-{digest}", topic["module"], difficulty,
        f"请以 Java 后端面试的方式，系统说明「{name}」：它解决什么问题、核心机制是什么，以及工程实践中需要注意哪些边界？",
        json.dumps(["核心概念与目标", "关键流程或底层机制", "适用场景和设计取舍", "故障边界与优化方式"], ensure_ascii=False),
        json.dumps(["只给结论不讲原理", "遗漏前提与边界", "不能联系后端工程场景"], ensure_ascii=False),
        json.dumps([f"{name} 的关键实现或执行流程是什么？", f"什么场景下需要重点关注 {name}？"], ensure_ascii=False),
        json.dumps([f"线上服务出现与 {name} 相关的性能或稳定性问题时，你会如何定位并处理？"], ensure_ascii=False),
        json.dumps(["概念准确性", "机制完整性", "工程实践与边界", "表达条理"], ensure_ascii=False),
        f"xiaolincoding,{topic['module'].lower()},source-topic", "1",
    ]
    values = ["'" + escape(value) + "'" for value in fields[:-1]] + [fields[-1]]
    return "(" + ",\n ".join(values) + ")"


def write(topics: list[dict[str, str]]) -> None:
    SOURCE_FILE.write_text(json.dumps(topics, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    output = "-- Generated from public topic titles/headings; source article prose is excluded.\n"
    output += "INSERT IGNORE INTO question_card\n(card_code, module, difficulty, main_question, key_points, common_mistakes, followups, scenario_followups, scoring_rubric, tags, enabled)\nVALUES\n"
    SQL_FILE.write_text(output + ",\n\n".join(render(topic) for topic in topics) + ";\n", encoding="utf-8")


if __name__ == "__main__":
    if "--from-cache" in sys.argv:
        collected = json.loads(SOURCE_FILE.read_text(encoding="utf-8"))
        for topic in collected:
            topic["heading"] = clean(topic["heading"])
    else:
        collected = discover()
    write(collected)
    print(f"Generated {len(collected)} question cards.")
