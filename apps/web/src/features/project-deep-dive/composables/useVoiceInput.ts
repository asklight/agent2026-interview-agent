import { computed, ref } from 'vue'

export function useVoiceInput() {
  const status = ref<'idle' | 'unavailable' | 'recording' | 'transcribing'>('idle')
  const transcript = ref('')
  const isSupported = computed(() => false)

  function start() {
    status.value = 'unavailable'
  }

  function stop() {
    status.value = 'idle'
  }

  function reset() {
    transcript.value = ''
    status.value = 'idle'
  }

  return { status, transcript, isSupported, start, stop, reset }
}
