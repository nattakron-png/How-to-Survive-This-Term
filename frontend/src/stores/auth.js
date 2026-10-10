import { computed, ref } from 'vue'

const STORAGE_KEY = 'tournament-hub-admin'

function readStoredUser() {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

const currentUser = ref(readStoredUser())

export function useAuth() {
  const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')

  function setUser(user) {
    currentUser.value = user
    try {
      if (user) sessionStorage.setItem(STORAGE_KEY, JSON.stringify(user))
      else sessionStorage.removeItem(STORAGE_KEY)
    } catch {
      return
    }
  }

  function logout() {
    setUser(null)
  }

  return { currentUser, isAdmin, setUser, logout }
}
