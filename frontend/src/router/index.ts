import { createRouter, createWebHistory } from '@ionic/vue-router';
import type { RouteRecordRaw } from 'vue-router';
import HomePage from '@/views/HomePage.vue';
import LoginPage from '@/views/LoginPage.vue';
import RegisterPage from '@/views/RegisterPage.vue';
import GroupDetailPage from '@/views/GroupDetailPage.vue';
import AddExpensePage from '@/views/AddExpensePage.vue';
import SettlementsPage from '@/views/SettlementsPage.vue';
import { useAuthStore } from '@/stores/auth';

// Routes reachable without authentication. Extend as new public pages appear.
const PUBLIC_ROUTES = ['/login', '/register'];

// Routes are extended per phase (groups, expenses, settlements...).
const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/home' },
  { path: '/home', name: 'home', component: HomePage },
  { path: '/groups/:id', name: 'group-detail', component: GroupDetailPage, props: true },
  { path: '/groups/:id/expenses/add', name: 'add-expense', component: AddExpensePage, props: true },
  { path: '/groups/:id/settlements', name: 'settlements', component: SettlementsPage, props: true },
  { path: '/login', name: 'login', component: LoginPage },
  { path: '/register', name: 'register', component: RegisterPage },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  const isPublic = PUBLIC_ROUTES.includes(to.path);

  if (!auth.isAuthenticated && !isPublic) {
    return { path: '/login' };
  }
  if (auth.isAuthenticated && isPublic) {
    return { path: '/home' };
  }
  return true;
});

export default router;
