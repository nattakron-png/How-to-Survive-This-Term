import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import TournamentsView from '@/views/TournamentsView.vue'
import TournamentDetailView from '@/views/TournamentDetailView.vue'
import MatchDetailView from '@/views/MatchDetailView.vue'

const routes = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/tournaments', name: 'tournaments', component: TournamentsView },
  { path: '/tournaments/:id', name: 'tournament-detail', component: TournamentDetailView, props: true },
  { path: '/tournaments/:id/matches/:matchId', name: 'match-detail', component: MatchDetailView, props: true },
]

export default createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: (to, from, saved) => saved ?? (to.path !== from.path ? { top: 0 } : false),
})
