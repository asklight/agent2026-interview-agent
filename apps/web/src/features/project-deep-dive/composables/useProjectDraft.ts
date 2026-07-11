import { ref, watch, type Ref } from 'vue'

export const PROJECT_DRAFT_STORAGE_KEY = 'agent2026:project-deep-dive:draft'

function storageAvailable() {
  return typeof window !== 'undefined' && Boolean(window.sessionStorage)
}

export function readProjectDraft() {
  return storageAvailable() ? window.sessionStorage.getItem(PROJECT_DRAFT_STORAGE_KEY) ?? '' : ''
}

export function writeProjectDraft(value: string) {
  if (!storageAvailable()) return
  if (value) window.sessionStorage.setItem(PROJECT_DRAFT_STORAGE_KEY, value)
  else window.sessionStorage.removeItem(PROJECT_DRAFT_STORAGE_KEY)
}

export function useProjectDraft(): { draft: Ref<string>; clearDraft: () => void } {
  const draft = ref(readProjectDraft())
  watch(draft, value => writeProjectDraft(value), { flush: 'sync' })

  return {
    draft,
    clearDraft: () => {
      draft.value = ''
      writeProjectDraft('')
    },
  }
}
