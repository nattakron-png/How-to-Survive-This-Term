import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import TournamentsView from '@/views/TournamentsView.vue'

const routes = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/tournaments', name: 'tournaments', component: TournamentsView },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
