import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import TournamentsView from '@/views/TournamentsView.vue'
import TournamentDetailView from '@/views/TournamentDetailView.vue'
import MatchDetailView from '@/views/MatchDetailView.vue'
import AdminLoginView from '@/views/admin/AdminLoginView.vue'
import AdminHomeView from '@/views/admin/AdminHomeView.vue'
import AdminTournamentsView from '@/views/admin/AdminTournamentsView.vue'
import AdminTournamentFormView from '@/views/admin/AdminTournamentFormView.vue'
import AdminTournamentTeamsView from '@/views/admin/AdminTournamentTeamsView.vue'
import AdminTeamsView from '@/views/admin/AdminTeamsView.vue'
import AdminTeamFormView from '@/views/admin/AdminTeamFormView.vue'
import AdminPlayersView from '@/views/admin/AdminPlayersView.vue'
import AdminMatchesView from '@/views/admin/AdminMatchesView.vue'
import AdminFreeFireResultsView from '@/views/admin/AdminFreeFireResultsView.vue'
import { useAuth } from '@/stores/auth'

const routes = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/tournaments', name: 'tournaments', component: TournamentsView },
  { path: '/tournaments/:id', name: 'tournament-detail', component: TournamentDetailView, props: true },
  { path: '/tournaments/:id/matches/:matchId', name: 'match-detail', component: MatchDetailView, props: true },
  { path: '/admin/login', name: 'admin-login', component: AdminLoginView, meta: { guestOnly: true } },
  { path: '/admin', name: 'admin-home', component: AdminHomeView, meta: { requiresAdmin: true } },
  { path: '/admin/tournaments', name: 'admin-tournaments', component: AdminTournamentsView, meta: { requiresAdmin: true } },
  { path: '/admin/tournaments/new', name: 'admin-tournament-new', component: AdminTournamentFormView, meta: { requiresAdmin: true } },
  { path: '/admin/tournaments/:id/edit', name: 'admin-tournament-edit', component: AdminTournamentFormView, props: true, meta: { requiresAdmin: true } },
  { path: '/admin/tournaments/:id/teams', name: 'admin-tournament-teams', component: AdminTournamentTeamsView, props: true, meta: { requiresAdmin: true } },
  { path: '/admin/teams', name: 'admin-teams', component: AdminTeamsView, meta: { requiresAdmin: true } },
  { path: '/admin/teams/new', name: 'admin-team-new', component: AdminTeamFormView, meta: { requiresAdmin: true } },
  { path: '/admin/teams/:id/edit', name: 'admin-team-edit', component: AdminTeamFormView, props: true, meta: { requiresAdmin: true } },
  { path: '/admin/players', name: 'admin-players', component: AdminPlayersView, meta: { requiresAdmin: true } },
  { path: '/admin/matches', name: 'admin-matches', component: AdminMatchesView, meta: { requiresAdmin: true } },
  { path: '/admin/tournaments/:id/games', name: 'admin-tournament-games', component: AdminFreeFireResultsView, props: true, meta: { requiresAdmin: true } },
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
