<template>
  <section class="login">
    <h1>Sign in</h1>
    <p class="muted">Authenticates against <code>POST /api/auth/login</code> on the gateway.</p>

    <form @submit.prevent="onSubmit">
      <label for="email">Email</label>
      <input id="email" v-model="email" type="email" autocomplete="username" required />

      <label for="password">Password</label>
      <input id="password" v-model="password" type="password" autocomplete="current-password" required />

      <div v-if="error" class="error-banner">{{ error }}</div>

      <div class="actions">
        <button type="submit" :disabled="submitting">
          {{ submitting ? 'Signing in…' : 'Sign in' }}
        </button>
        <RouterLink to="/signup" class="muted">No account? Sign up</RouterLink>
      </div>
    </form>
  </section>
</template>

<script>
import { RouterLink } from 'vue-router';
import { api, ApiError } from '../api/client.js';
import { isAuthenticated, setCurrentUser, setToken } from '../auth/token.js';

export default {
  name: 'LoginView',
  components: { RouterLink },
  data() {
    return {
      email: '',
      password: '',
      submitting: false,
      error: ''
    };
  },
  methods: {
    async onSubmit() {
      console.log('[LoginView] onSubmit called');
      console.log('[LoginView] Form data:', { email: this.email });
      this.error = '';
      this.submitting = true;
      try {
        console.log('[LoginView] Sending POST /api/auth/login');
        const result = await api.post('/api/auth/login', {
          email: this.email,
          password: this.password
        });
        console.log('[LoginView] Login response:', result);
        const token = result && (result.token || result.accessToken || result.jwt);
        console.log('[LoginView] Token extracted:', token ? 'YES' : 'NO');
        console.log('[LoginView] User data:', result && result.user);
        if (!token) {
          this.error = 'Login response did not include a token.';
          return;
        }
        setToken(token);
        console.log('[LoginView] Token set in localStorage, now setting user');
        setCurrentUser(result && result.user ? result.user : null);
        console.log('[LoginView] setCurrentUser called, checking isAuthenticated():', isAuthenticated());
        const next = typeof this.$route.query.next === 'string' ? this.$route.query.next : '/';
        console.log('[LoginView] Redirecting to:', next);
        this.$router.push(next);
      } catch (err) {
        console.error('[LoginView] Login error:', err);
        this.error = err instanceof ApiError ? err.message : 'Sign-in failed.';
      } finally {
        this.submitting = false;
      }
    }
  }
};
</script>

<style scoped>
.login { max-width: 420px; }
.actions { display: flex; align-items: center; gap: 1rem; margin-top: 1rem; }
.actions a { color: var(--qb-muted); }
</style>
