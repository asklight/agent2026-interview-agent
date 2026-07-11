import { defineStore } from 'pinia'
import type { InputModality } from '@/features/project-deep-dive/model/types'

const PROFILE_TOKENS_KEY = 'agent2026:project-deep-dive:profile-tokens'
const SESSION_ACCESS_KEY = 'agent2026:project-deep-dive:sessions'

interface SessionAccessRecord {
  profileId: number
  accessToken: string
}

function readMap<T>(key: string): Record<string, T> {
  if (typeof window === 'undefined') return {}
  try {
    return JSON.parse(window.sessionStorage.getItem(key) || '{}') as Record<string, T>
  } catch {
    window.sessionStorage.removeItem(key)
    return {}
  }
}

function writeMap<T>(key: string, value: Record<string, T>) {
  if (typeof window !== 'undefined') window.sessionStorage.setItem(key, JSON.stringify(value))
}

export const useInterviewSessionStore = defineStore('project-interview-session', {
  state: () => ({
    profileId: null as number | null,
    sessionId: null as number | null,
    accessToken: '',
    conversationPhase: '',
    connectionStatus: 'idle' as 'idle' | 'connecting' | 'online' | 'offline',
    inputModality: 'TEXT' as InputModality,
  }),
  actions: {
    saveProfileAccess(profileId: number, accessToken: string) {
      const tokens = readMap<string>(PROFILE_TOKENS_KEY)
      tokens[String(profileId)] = accessToken
      writeMap(PROFILE_TOKENS_KEY, tokens)
      this.profileId = profileId
      this.accessToken = accessToken
    },
    restoreProfileAccess(profileId: number) {
      const accessToken = readMap<string>(PROFILE_TOKENS_KEY)[String(profileId)] || ''
      this.profileId = profileId
      this.accessToken = accessToken
      return accessToken
    },
    saveSessionAccess(sessionId: number, profileId: number, accessToken: string) {
      const sessions = readMap<SessionAccessRecord>(SESSION_ACCESS_KEY)
      sessions[String(sessionId)] = { profileId, accessToken }
      writeMap(SESSION_ACCESS_KEY, sessions)
      this.sessionId = sessionId
      this.profileId = profileId
      this.accessToken = accessToken
    },
    restoreSessionAccess(sessionId: number) {
      const record = readMap<SessionAccessRecord>(SESSION_ACCESS_KEY)[String(sessionId)]
      this.sessionId = sessionId
      if (!record) return null
      this.profileId = record.profileId
      this.accessToken = record.accessToken
      return record
    },
    setInputModality(value: InputModality) {
      this.inputModality = value
    },
  },
})

export { PROFILE_TOKENS_KEY, SESSION_ACCESS_KEY }
