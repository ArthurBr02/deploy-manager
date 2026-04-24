import { onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import axios from '@/api/axios'

export function useDeploymentEvents(onEvent) {
  let src = null
  let destroyed = false
  let retryTimer = null

  const connect = () => {
    if (destroyed) return
    axios.post('/deployments/sse-token').then(({ data }) => {
      if (destroyed) return
      const token = data.token

      src = new EventSource(`/api/deployments/events?token=${token}`)

      src.addEventListener('open', () => console.log('[SSE] connected'))
      src.addEventListener('deployment.status', e => {
        try {
          const parsed = JSON.parse(e.data)
          onEvent(parsed)
        } catch (err) {
          console.error('[SSE] parse error:', err, e.data)
        }
      })
      src.addEventListener('error', e => {
        console.error('[SSE] error:', e)
        if (src) { src.close(); src = null }
        if (!destroyed) retryTimer = setTimeout(connect, 3000)
      })
    }).catch(err => {
      console.error('[SSE] failed to get token:', err)
    })
  }

  onMounted(() => {
    connect()
  })

  onUnmounted(() => {
    destroyed = true
    clearTimeout(retryTimer)
    if (src) { src.close(); src = null }
  })
}
