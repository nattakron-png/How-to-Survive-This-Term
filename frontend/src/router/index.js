import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import TournamentsView from '@/views/TournamentsView.vue'
import TournamentDetailView from '@/views/TournamentDetailView.vue'
import MatchDetailView from '@/views/MatchDetailView.vue'
import AdminLoginView from '@/views/admin/AdminLoginView.vue'
import AdminHomeView from '@/views/admin/AdminHomeView.vue'
import { useAuth } from '@/stores/auth'

const routes = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/tournaments', name: 'tournaments', component: TournamentsView },
  { path: '/tournaments/:id', name: 'tournament-detail', component: TournamentDetailView, props: true },
  { path: '/tournaments/:id/matches/:matchId', name: 'match-detail', component: MatchDetailView, props: true },
  { path: '/admin/login', name: 'admin-login', component: AdminLoginView, meta: { guestOnly: true } },
  { path: '/admin', name: 'admin-home', component: AdminHomeView, meta: { requiresAdmin: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: (to, from, saved) => saved ?? (to.path !== from.path ? { top: 0 } : false),
})

router.beforeEach((to) => {
  const { isAdmin } = useAuth()
  if (to.matched.some((r) => r.meta.requiresAdmin) && !isAdmin.value) {
    return { name: 'admin-login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && isAdmin.value) {
    return { name: 'admin-home' }
  }
  return true
})

export default router
