import { onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

export function useDeploymentEvents(onEvent) {
  let src = null

  onMounted(() => {
    const auth = useAuthStore()
    src = new EventSource(`/api/deployments/events?token=${auth.accessToken}`)
    src.addEventListener('open', () => console.log('[SSE] connected'))
    src.addEventListener('deployment.status', e => {
      console.log('[SSE] deployment.status raw:', e.data)
      try {
        const parsed = JSON.parse(e.data)
        console.log('[SSE] deployment.status parsed:', parsed)
        onEvent(parsed)
      } catch (err) {
        console.error('[SSE] parse error:', err, e.data)
      }
    })
    src.addEventListener('error', e => console.error('[SSE] error:', e))
  })

  onUnmounted(() => {
    if (src) { src.close(); src = null }
  })
}
