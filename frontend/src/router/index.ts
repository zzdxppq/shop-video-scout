/**
 * Vue Router configuration.
 */
import { createRouter, createWebHistory } from 'vue-router';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      redirect: '/create'
    },
    {
      path: '/create',
      name: 'create-task',
      component: () => import('../views/CreateTaskView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/task/:id/upload',
      name: 'task-upload',
      component: () => import('../views/TaskUploadView.vue'),
      meta: { requiresAuth: true }
    }
  ]
});

// Navigation guard for auth
router.beforeEach((to, _from, next) => {
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
  const token = localStorage.getItem('access_token');

  if (requiresAuth && !token) {
    next('/login');
  } else {
    next();
  }
});

export default router;
