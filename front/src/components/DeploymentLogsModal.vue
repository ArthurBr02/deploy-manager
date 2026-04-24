<template>
  <div class="fixed inset-0 bg-black/40 z-40 flex items-center justify-center p-4" @click.self="close">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-3xl flex flex-col max-h-[85vh] overflow-hidden">
      <div class="px-5 py-4 border-b border-warm-border flex items-start justify-between gap-4">
        <div class="flex flex-wrap items-center gap-x-4 gap-y-1.5 min-w-0">
          <span class="font-semibold text-gray-900 font-mono text-sm">#{{ shortId(deployment.id) }}</span>
          <TypeBadge :type="deployment.type" />
          <StatusBadge :status="currentStatus" />
          <UserBadge :user="{ firstName: deployment.userFirstName, lastName: deployment.userLastName, avatar: deployment.userAvatar }" />
          <span class="text-sm text-gray-400">{{ formatDate(deployment.createdAt) }}</span>
          <span v-if="deployment.durationSeconds != null" class="text-xs font-mono text-gray-400">{{ formatDuration(deployment.durationSeconds) }}</span>
          <span v-if="isStreaming" class="flex items-center gap-1.5 text-status-progress text-xs animate-pulse">
            <span class="w-1.5 h-1.5 rounded-full bg-status-progress inline-block"></span>
            En cours…
          </span>
        </div>
        <button @click="close" class="p-1 rounded hover:bg-warm-muted text-gray-400 hover:text-gray-600 flex-shrink-0">
          <XIcon class="w-4 h-4" />
        </button>
      </div>

      <div class="dm-term flex-1 overflow-auto min-h-0" ref="logEl" style="min-height: 200px; max-height: 60vh;">{{ logContent || '(aucun log)' }}</div>
    </div>
  </div>
</template>

<script>
import { mapState } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import StatusBadge from '@/components/StatusBadge.vue'
import TypeBadge from '@/components/TypeBadge.vue'
import UserBadge from '@/components/UserBadge.vue'
import { XIcon } from '@/components/icons'
import axios from '@/api/axios'

export default {
  components: { StatusBadge, TypeBadge, UserBadge, XIcon },
  props: {
    deployment: { type: Object, required: true },
  },
  emits: ['close'],
  computed: {
    ...mapState(useAuthStore, ['accessToken']),
  },
  data() {
    return {
      logContent: '',
      isStreaming: false,
      currentStatus: this.deployment.status,
      _sse: null,
    }
  },
  mounted() {
    if (this.deployment.status === 'IN_PROGRESS') {
      this.startSse()
    } else {
      this.logContent = this.deployment.logs || ''
      this.$nextTick(this.scrollBottom)
    }
  },
  beforeUnmount() {
    this.stopSse()
  },
  methods: {
    close() {
      this.stopSse()
      this.$emit('close')
    },
    startSse() {
      this.isStreaming = true
      this.currentStatus = 'IN_PROGRESS'
      axios.post('/deployments/sse-token').then(({ data }) => {
        const token = data.token
        const src = new EventSource(`/api/deployments/${this.deployment.id}/logs?token=${token}`)
        src.addEventListener('log', e => {
          this.logContent += e.data
          this.$nextTick(this.scrollBottom)
        })
        src.addEventListener('end', e => {
          this.isStreaming = false
          this.currentStatus = e.data !== 'done' ? e.data : 'SUCCESS'
          src.close()
          this._sse = null
        })
        src.addEventListener('error', () => {
          this.isStreaming = false
          src.close()
          this._sse = null
        })
        this._sse = src
      }).catch(err => {
        console.error('[SSE] Failed to get log token', err)
        this.isStreaming = false
      })
    },
    stopSse() {
      if (this._sse) {
        this._sse.close()
        this._sse = null
      }
    },
    scrollBottom() {
      if (this.$refs.logEl) this.$refs.logEl.scrollTop = this.$refs.logEl.scrollHeight
    },
    shortId(id) {
      return id ? String(id).slice(0, 6) : '—'
    },
    formatDate(d) {
      if (!d) return '—'
      return new Date(d).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })
    },
    formatDuration(seconds) {
      if (seconds == null) return '—'
      const m = Math.floor(seconds / 60).toString().padStart(2, '0')
      const s = (seconds % 60).toString().padStart(2, '0')
      return `${m}:${s}`
    },
  },
}
</script>
