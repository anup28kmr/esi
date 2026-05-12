<template>
  <section class="login">
    <h1>Sign in</h1>

    <form @submit.prevent="onSubmit">
      <label for="email">Email</label>
      <input id="email" v-model="email" type="email" autocomplete="username" required />

      <label for="password">Password</label>
      <input id="password" v-model="password" type="password" autocomplete="current-password" required />

      <div v-if="success" class="success-banner">{{ success }}</div>
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
      error: '',
      success: ''
    };
  },
  created() {
    if (this.$route.query.registered === '1') {
      this.success = 'Account created. Please sign in.';
    }
  },
  methods: {
    async onSubmit() {
      console.log('[LoginView] onSubmit called');
      console.log('[LoginView] Form data:', { email: this.email });
      this.error = '';
      this.success = '';
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
        this.error = this.describeLoginError(err);
      } finally {
        this.submitting = false;
      }
    },
    describeLoginError(err) {
      if (!(err instanceof ApiError)) {
        return 'Sign-in failed. Please try again.';
      }
      const invalidCreds = 'Invalid email or password. Please check your credentials and try again.';
      // Single message for both "no such user" and "wrong password" so we
      // don't leak which emails are registered.
      if (err.status === 401 || err.status === 403 || err.status === 404) {
        return invalidCreds;
      }
      // The user-service currently lets Spring Security's AuthenticationException
      // fall through to the catch-all 500 handler, so bad credentials arrive as a
      // 500 with a body like {"message":"Bad credentials"}. Pattern-match the
      // body so the user still sees the right thing.
      const bodyMessage =
        (err.body && typeof err.body === 'object' && (err.body.message || err.body.error)) ||
        err.message ||
        '';
      if (/bad credentials|invalid access|invalid credentials|user.*not found|no such user/i.test(bodyMessage)) {
        return invalidCreds;
      }
      if (err.status === 0) {
        return 'Cannot reach the server. Please check your connection and try again.';
      }
      if (err.status >= 500) {
        return 'The sign-in service is temporarily unavailable. Please try again in a moment.';
      }
      return bodyMessage || 'Sign-in failed. Please try again.';
    }
  }
};
</script>

<style scoped>
.login { max-width: 420px; }
.actions { display: flex; align-items: center; gap: 1rem; margin-top: 1rem; }
.actions a { color: var(--qb-muted); }
.success-banner {
  background: #dcfce7;
  color: #166534;
  border: 1px solid #86efac;
  padding: 0.5rem 0.8rem;
  border-radius: 4px;
  margin: 0.75rem 0 0;
}
</style>
