<template>
  <div class="flex flex-col h-full bg-[#1a1a1a]">
    <header class="h-12 border-b border-white/10 bg-[#242424] flex items-center px-4 gap-4 flex-shrink-0">
      <RouterLink :to="`/hosts/${$route.params.id}`" class="text-gray-400 hover:text-white flex items-center gap-1 text-sm transition-colors">
        <ArrowLeftIcon class="w-4 h-4" /> Retour à l'hôte
      </RouterLink>
      <div class="h-4 w-px bg-white/10" />
      <div class="text-white text-sm font-medium flex items-center gap-2">
        <TerminalIcon class="w-4 h-4 text-accent" />
        Terminal — {{ hostName }}
      </div>
      <div class="flex-1" />
      <div class="flex items-center gap-2">
        <span class="text-[10px] uppercase font-bold px-2 py-0.5 rounded-full" 
          :class="connected ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'">
          {{ connected ? 'Connecté' : 'Déconnecté' }}
        </span>
      </div>
    </header>
    <div class="flex-1 p-2 overflow-hidden" ref="terminalContainer">
      <div id="terminal" class="h-full w-full"></div>
    </div>
  </div>
</template>

<script>
import { Terminal } from 'xterm'
import { FitAddon } from 'xterm-addon-fit'
import 'xterm/css/xterm.css'
import { mapState } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import hostsService from '@/services/hostsService'
import { ArrowLeftIcon, TerminalIcon } from '@/components/icons'

export default {
  components: { ArrowLeftIcon, TerminalIcon },
  computed: {
    ...mapState(useAuthStore, ['accessToken']),
  },
  data() {
    return {
      term: null,
      fitAddon: null,
      ws: null,
      connected: false,
      hostName: '',
    }
  },
  mounted() {
    this.loadHost()
    this.initTerminal()
    this.connect()
    window.addEventListener('resize', this.onResize)
  },
  unmounted() {
    window.removeEventListener('resize', this.onResize)
    if (this.ws) this.ws.close()
    if (this.term) {
      // fitAddon is disposed automatically when term is disposed if it was loaded
      this.term.dispose()
    }
  },
  methods: {
    loadHost() {
      hostsService.getById(this.$route.params.id).then(res => {
        this.hostName = res.data.name
      })
    },
    initTerminal() {
      this.term = new Terminal({
        cursorBlink: true,
        fontSize: 13,
        fontFamily: 'JetBrains Mono, Menlo, Monaco, Consolas, Courier New, monospace',
        theme: {
          background: '#1a1a1a',
          foreground: '#d4d4d4',
        },
      })
      this.fitAddon = new FitAddon()
      this.term.loadAddon(this.fitAddon)
      this.term.open(document.getElementById('terminal'))
      this.fitAddon.fit()
      
      this.term.onData(data => {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
          this.ws.send(data)
        }
      })
    },
    connect() {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const hostId = this.$route.params.id
      const url = `${protocol}//${window.location.host}/api/ws/terminal?token=${this.accessToken}&hostId=${hostId}`
      
      this.ws = new WebSocket(url)
      
      this.ws.onopen = () => {
        this.connected = true
        this.term.write('\r\n*** Session SSH établie ***\r\n')
      }
      
      this.ws.onmessage = (event) => {
        this.term.write(event.data)
      }
      
      this.ws.onclose = () => {
        this.connected = false
        this.term.write('\r\n*** Session terminée ***\r\n')
      }
      
      this.ws.onerror = () => {
        this.term.write('\r\n[ERROR] Erreur de connexion WebSocket\r\n')
      }
    },
    onResize() {
      this.fitAddon?.fit()
    },
  },
}
</script>

<style>
.xterm .xterm-viewport {
  background-color: #1a1a1a !initial !important;
}
</style>
