import axios from '@/api/axios'

/**
 * Composable to handle deployment status events via SSE.
 * It automatically handles token generation, connection, and retries.
 * 
 * @param {Function} onEvent Callback called when a deployment status event is received.
 * @returns {Object} { connect, disconnect }
 */
export function useDeploymentEvents(onEvent, onReconnect = null) {
  let src = null
  let destroyed = false
  let retryTimer = null
  let hadError = false

  const disconnect = () => {
    destroyed = true
    hadError = false
    clearTimeout(retryTimer)
    if (src) {
      src.close()
      src = null
    }
  }

  const connect = () => {
    destroyed = false
    if (src) {
      src.close()
      src = null
    }

    axios.post('/deployments/sse-token').then(({ data }) => {
      if (destroyed) return
      const token = data.token

      src = new EventSource(`/api/deployments/events?token=${token}`)

      src.addEventListener('open', () => {
        console.log('[SSE] Global events connected')
        if (hadError && onReconnect) {
          hadError = false
          onReconnect()
        }
      })
      
      src.addEventListener('deployment.status', e => {
        try {
          const parsed = JSON.parse(e.data)
          onEvent(parsed)
        } catch (err) {
          console.error('[SSE] parse error:', err, e.data)
        }
      })

      src.addEventListener('error', e => {
        console.error('[SSE] Global events error:', e)
        hadError = true
        if (src) {
          src.close()
          src = null
        }
        if (!destroyed) {
          retryTimer = setTimeout(connect, 3000)
        }
      })
    }).catch(err => {
      console.error('[SSE] failed to get token:', err)
      if (!destroyed) {
        retryTimer = setTimeout(connect, 5000)
      }
    })
  }

  return { connect, disconnect }
}
