/**
 * Utility to sync component filters with URL query parameters and localStorage.
 * Designed for Vue 3 Options API.
 */
import router from '@/router'

export function syncQuery(component, options) {
  const { 
    key,            // localStorage key
    defaultFilters, // default values
    onUpdate        // function to call when filters change (e.g. this.load)
  } = options

  // 1. Load from URL or LocalStorage
  const query = router.currentRoute.value.query
  const saved = localStorage.getItem(`cache_v1_${key}`)
  const initial = saved ? JSON.parse(saved) : { ...defaultFilters }

  // Overwrite with URL params
  Object.keys(defaultFilters).forEach(k => {
    if (query[k] !== undefined) {
      // Basic type conversion
      const val = query[k]
      if (typeof defaultFilters[k] === 'number') initial[k] = Number(val)
      else if (typeof defaultFilters[k] === 'boolean') initial[k] = val === 'true'
      else initial[k] = val
    }
  })

  // 2. Set initial state on component
  Object.assign(component.filters, initial)

  // 3. Watch for changes
  component.$watch('filters', (newVal) => {
    // Save to localStorage
    localStorage.setItem(`cache_v1_${key}`, JSON.stringify(newVal))

    // Update URL without reloading
    const newQuery = { ...router.currentRoute.value.query }
    Object.keys(newVal).forEach(k => {
      if (newVal[k] !== defaultFilters[k] && newVal[k] !== '' && newVal[k] !== null) {
        newQuery[k] = String(newVal[k])
      } else {
        delete newQuery[k]
      }
    })

    router.replace({ query: newQuery }).catch(() => {})

    // Trigger update
    if (onUpdate) onUpdate.call(component, newVal)
  }, { deep: true })
}
