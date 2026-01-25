/**
 * Vue Router configuration.
 * Story 1.3: Frontend Login Page - T7
 *
 * Features:
 * - Auth route guard with redirect preservation
 * - Login page route
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
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { requiresAuth: false }
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
    },
    {
      path: '/task/:id',
      name: 'task-analysis',
      component: () => import('../views/TaskAnalysisView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/task/:id/script',
      name: 'task-script',
      component: () => import('../views/ScriptEditorView.vue'),
      meta: { requiresAuth: true }
    }
  ]
});

/**
 * Navigation guard for authentication.
 * T7: Route guard with redirect preservation
 * - Blocks unauthenticated access to protected routes
 * - Saves original URL in redirect query param
 * - Redirects to original URL after login
 */
router.beforeEach((to, _from, next) => {
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
  const token = localStorage.getItem('access_token');

  // If route requires auth and no token
  if (requiresAuth && !token) {
    // Preserve original destination in redirect query
    const redirect = to.fullPath;
    next({
      path: '/login',
      query: { redirect }
    });
  }
  // If already logged in and trying to access login page
  else if (to.path === '/login' && token) {
    // Redirect to home or the redirect query param
    const redirect = (to.query.redirect as string) || '/';
    next(redirect);
  }
  else {
    next();
  }
});

export default router;
