<template>
  <Teleport to="body">
    <div v-if="show" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>
      <div class="relative bg-white rounded-xl shadow-xl w-full max-w-3xl max-h-[85vh] flex flex-col z-10">
        <!-- Header -->
        <div class="flex items-center justify-between px-5 py-4 border-b border-warm-border flex-shrink-0">
          <div class="flex items-center gap-3">
            <span :class="actionClass" class="px-2 py-0.5 rounded text-[10px] font-bold uppercase">{{ log.action }}</span>
            <span class="font-semibold text-gray-900">{{ log.entityName }}</span>
            <span v-if="log.entityId" class="text-xs text-gray-400 font-mono hidden sm:inline">{{ log.entityId }}</span>
          </div>
          <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600 transition-colors p-1 rounded-md hover:bg-warm-muted">
            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
          </button>
        </div>

        <!-- Body -->
        <div class="overflow-y-auto flex-1 p-5 space-y-4">
          <!-- Terminal events -->
          <template v-if="isTerminalEvent">
            <div class="space-y-2">
              <template v-if="parsedNew">
                <div v-for="(value, key) in parsedNew" :key="key" class="flex gap-3 text-sm">
                  <span class="w-40 text-gray-500 flex-shrink-0 font-medium">{{ formatKey(key) }}</span>
                  <span class="font-mono text-gray-800 break-all">{{ value }}</span>
                </div>
              </template>
              <div v-else class="font-mono text-sm text-gray-600 whitespace-pre-wrap">{{ log.newValue }}</div>
            </div>
          </template>

          <!-- Permission diff -->
          <template v-else-if="isPermissionDiff">
            <div class="space-y-3">
              <p class="text-xs text-gray-500">
                <span class="font-medium">{{ permissionDiffRows.length }}</span> hôte(s) modifié(s)
              </p>
              <table class="w-full text-sm border-collapse">
                <thead>
                  <tr class="border-b border-warm-border">
                    <th class="text-left py-2 pr-4 text-xs font-medium text-gray-500 uppercase tracking-wide">Hôte</th>
                    <th class="text-center py-2 px-3 text-xs font-medium text-gray-500 uppercase tracking-wide">Deploy</th>
                    <th class="text-center py-2 px-3 text-xs font-medium text-gray-500 uppercase tracking-wide">Edit</th>
                    <th class="text-center py-2 px-3 text-xs font-medium text-gray-500 uppercase tracking-wide">SSH</th>
                    <th class="text-left py-2 pl-2 text-xs font-medium text-gray-500 uppercase tracking-wide">Statut</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in permissionDiffRows" :key="row.hostId"
                    :class="{
                      'bg-green-50': row.type === 'added',
                      'bg-red-50': row.type === 'removed',
                      'bg-yellow-50': row.type === 'modified',
                    }"
                    class="border-b border-warm-border/40">
                    <td class="py-2 pr-4 font-medium text-gray-800 text-xs">{{ row.hostName || row.hostId }}</td>
                    <td class="py-2 px-3 text-center">
                      <span v-html="permFlag(row, 'canDeploy')"></span>
                    </td>
                    <td class="py-2 px-3 text-center">
                      <span v-html="permFlag(row, 'canEdit')"></span>
                    </td>
                    <td class="py-2 px-3 text-center">
                      <span v-html="permFlag(row, 'canExecute')"></span>
                    </td>
                    <td class="py-2 pl-2 text-xs font-medium">
                      <span v-if="row.type === 'added'" class="text-green-700">Ajouté</span>
                      <span v-else-if="row.type === 'removed'" class="text-red-700">Retiré</span>
                      <span v-else class="text-amber-700">Modifié</span>
                    </td>
                  </tr>
                  <tr v-if="permissionDiffRows.length === 0">
                    <td colspan="5" class="py-4 text-center text-gray-400 italic text-xs">Aucun changement détecté</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>

          <!-- Diff table for UPDATE/CREATE/DELETE with JSON values -->
          <template v-else-if="hasDiff">
            <table class="w-full text-sm border-collapse">
              <thead>
                <tr class="border-b border-warm-border">
                  <th class="text-left py-2 pr-4 text-xs font-medium text-gray-500 uppercase tracking-wide w-1/4">Champ</th>
                  <th class="text-left py-2 pr-4 text-xs font-medium text-gray-500 uppercase tracking-wide w-[37.5%]">Avant</th>
                  <th class="text-left py-2 text-xs font-medium text-gray-500 uppercase tracking-wide w-[37.5%]">Après</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="key in allKeys" :key="key"
                  :class="isChanged(key) ? 'bg-yellow-50' : ''"
                  class="border-b border-warm-border/40">
                  <td class="py-2 pr-4 font-medium text-gray-700 align-top">{{ formatKey(key) }}</td>
                  <td class="py-2 pr-4 font-mono text-xs align-top break-all">
                    <span v-if="parsedOld && parsedOld[key] != null"
                      :class="isChanged(key) ? 'bg-red-100 text-red-800 px-1 rounded' : 'text-gray-500'">
                      {{ parsedOld[key] }}
                    </span>
                    <span v-else class="text-gray-300">—</span>
                  </td>
                  <td class="py-2 font-mono text-xs align-top break-all">
                    <span v-if="parsedNew && parsedNew[key] != null"
                      :class="isChanged(key) ? 'bg-green-100 text-green-800 px-1 rounded' : 'text-gray-700'">
                      {{ parsedNew[key] }}
                    </span>
                    <span v-else class="text-gray-300">—</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </template>

          <!-- Raw text fallback -->
          <template v-else>
            <div v-if="log.oldValue" class="space-y-1">
              <p class="text-xs font-medium text-gray-500 uppercase tracking-wide">Ancienne valeur</p>
              <pre class="bg-warm-muted rounded-md p-3 text-xs font-mono whitespace-pre-wrap text-gray-600">{{ log.oldValue }}</pre>
            </div>
            <div v-if="log.newValue" class="space-y-1">
              <p class="text-xs font-medium text-gray-500 uppercase tracking-wide">Nouvelle valeur</p>
              <pre class="bg-warm-muted rounded-md p-3 text-xs font-mono whitespace-pre-wrap text-gray-600">{{ log.newValue }}</pre>
            </div>
            <p v-if="!log.oldValue && !log.newValue" class="text-gray-400 italic text-sm">Aucun détail disponible</p>
          </template>

          <!-- Metadata -->
          <div class="pt-3 border-t border-warm-border/40 text-xs text-gray-400 flex flex-wrap gap-4">
            <span>{{ formatDate(log.createdAt) }}</span>
            <span v-if="log.userEmail">Par {{ log.userFirstName }} {{ log.userLastName }} ({{ log.userEmail }})</span>
            <span v-if="log.entityId" class="font-mono sm:hidden">{{ log.entityId }}</span>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script>
const TERMINAL_ACTIONS = ['TERMINAL_CONNECT', 'TERMINAL_DISCONNECT', 'TERMINAL_COMMAND']

const CHECK = '<svg class="w-4 h-4 inline text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/></svg>'
const CROSS = '<svg class="w-4 h-4 inline text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12"/></svg>'
const DASH = '<span class="text-gray-300 text-base leading-none">—</span>'

function tryParse(str) {
  if (!str) return null
  try { return JSON.parse(str) } catch { return null }
}

export default {
  name: 'AuditDetailModal',
  emits: ['close'],
  props: {
    show: Boolean,
    log: { type: Object, default: null },
  },
  computed: {
    isTerminalEvent() {
      return this.log && TERMINAL_ACTIONS.includes(this.log.action)
    },
    parsedOld() { return this.log ? tryParse(this.log.oldValue) : null },
    parsedNew() { return this.log ? tryParse(this.log.newValue) : null },
    isPermissionDiff() {
      return Array.isArray(this.parsedOld?.permissions) && Array.isArray(this.parsedNew?.permissions)
    },
    permissionDiffRows() {
      if (!this.isPermissionDiff) return []
      const oldMap = new Map(this.parsedOld.permissions.map(p => [p.hostId, p]))
      const newMap = new Map(this.parsedNew.permissions.map(p => [p.hostId, p]))
      const rows = []

      for (const [hostId, p] of newMap) {
        if (!oldMap.has(hostId)) rows.push({ type: 'added', hostId, hostName: p.hostName, old: null, new: p })
      }
      for (const [hostId, p] of oldMap) {
        if (!newMap.has(hostId)) rows.push({ type: 'removed', hostId, hostName: p.hostName, old: p, new: null })
      }
      for (const [hostId, oldP] of oldMap) {
        if (!newMap.has(hostId)) continue
        const newP = newMap.get(hostId)
        if (oldP.canDeploy !== newP.canDeploy || oldP.canEdit !== newP.canEdit || oldP.canExecute !== newP.canExecute) {
          rows.push({ type: 'modified', hostId, hostName: oldP.hostName, old: oldP, new: newP })
        }
      }

      const order = { modified: 0, added: 1, removed: 2 }
      rows.sort((a, b) => order[a.type] - order[b.type] || (a.hostName ?? '').localeCompare(b.hostName ?? ''))
      return rows
    },
    hasDiff() {
      return this.parsedOld !== null || this.parsedNew !== null
    },
    allKeys() {
      const keys = new Set([
        ...Object.keys(this.parsedOld || {}),
        ...Object.keys(this.parsedNew || {}),
      ])
      return Array.from(keys)
    },
    actionClass() {
      if (!this.log) return 'bg-gray-100 text-gray-700'
      return {
        CREATE: 'bg-green-100 text-green-700',
        UPDATE: 'bg-blue-100 text-blue-700',
        DELETE: 'bg-red-100 text-red-700',
        TERMINAL_CONNECT: 'bg-purple-100 text-purple-700',
        TERMINAL_DISCONNECT: 'bg-gray-100 text-gray-600',
        TERMINAL_COMMAND: 'bg-orange-100 text-orange-700',
      }[this.log.action] || 'bg-gray-100 text-gray-700'
    },
  },
  methods: {
    isChanged(key) {
      const oldVal = this.parsedOld?.[key]
      const newVal = this.parsedNew?.[key]
      return String(oldVal ?? '') !== String(newVal ?? '')
    },
    permFlag(row, field) {
      if (row.type === 'added') return row.new[field] ? CHECK : DASH
      if (row.type === 'removed') return row.old[field] ? CHECK : DASH
      const wasTrue = row.old[field]
      const isTrue = row.new[field]
      if (wasTrue === isTrue) return wasTrue ? CHECK : DASH
      if (!wasTrue && isTrue) return `${DASH}&nbsp;<span class="text-gray-300 text-xs">→</span>&nbsp;${CHECK}`
      return `${CHECK}&nbsp;<span class="text-gray-300 text-xs">→</span>&nbsp;${CROSS}`
    },
    formatKey(key) {
      return key.replace(/([A-Z])/g, ' $1').replace(/_/g, ' ').trim()
    },
    formatDate(d) {
      return new Date(d).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })
    },
  },
}
</script>
