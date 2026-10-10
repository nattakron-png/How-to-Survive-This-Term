import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import TournamentsView from '@/views/TournamentsView.vue'
import TournamentDetailView from '@/views/TournamentDetailView.vue'

const routes = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/tournaments', name: 'tournaments', component: TournamentsView },
  { path: '/tournaments/:id', name: 'tournament-detail', component: TournamentDetailView, props: true },
]

export default createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})
