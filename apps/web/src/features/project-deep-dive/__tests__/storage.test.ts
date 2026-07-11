import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { readProjectDraft, writeProjectDraft, PROJECT_DRAFT_STORAGE_KEY } from '@/features/project-deep-dive/composables/useProjectDraft'
import { PROFILE_TOKENS_KEY, SESSION_ACCESS_KEY, useInterviewSessionStore } from '@/features/project-deep-dive/stores/interviewSession'

describe('project deep-dive session storage', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    setActivePinia(createPinia())
  })

  it('saves and clears the project draft in sessionStorage', () => {
    writeProjectDraft('Redis 项目经历草稿')
    expect(readProjectDraft()).toBe('Redis 项目经历草稿')
    expect(window.sessionStorage.getItem(PROJECT_DRAFT_STORAGE_KEY)).toBe('Redis 项目经历草稿')

    writeProjectDraft('')
    expect(readProjectDraft()).toBe('')
  })

  it('restores profile and interview access tokens without localStorage', () => {
    const store = useInterviewSessionStore()
    store.saveProfileAccess(12, 'profile-secret-token')
    store.saveSessionAccess(98, 12, 'profile-secret-token')

    const restoredStore = useInterviewSessionStore()
    expect(restoredStore.restoreProfileAccess(12)).toBe('profile-secret-token')
    expect(restoredStore.restoreSessionAccess(98)).toEqual({ profileId: 12, accessToken: 'profile-secret-token' })
    expect(window.sessionStorage.getItem(PROFILE_TOKENS_KEY)).toContain('profile-secret-token')
    expect(window.sessionStorage.getItem(SESSION_ACCESS_KEY)).toContain('profile-secret-token')
    expect(window.localStorage.length).toBe(0)
  })
})
