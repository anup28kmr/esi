<template>
  <nav class="app-nav">
    <div class="brand">
      <RouterLink to="/">QuickBite</RouterLink>
    </div>
    <ul class="links">
      <li><RouterLink to="/">Home</RouterLink></li>
      <li><RouterLink to="/restaurants">Restaurants</RouterLink></li>
      <li v-if="authed"><RouterLink to="/profile">User Profile</RouterLink></li>
      <li v-if="authed && !isOwner" class="cart-link">
        <RouterLink to="/cart">
          Cart
          <span v-if="cartCount > 0" class="cart-dot">{{ cartCount > 99 ? '99+' : cartCount }}</span>
        </RouterLink>
      </li>
      <li v-if="authed && !isDriver"><RouterLink to="/orders">Orders</RouterLink></li>
      <li v-if="authed && isDriver"><RouterLink to="/deliveries">Deliveries</RouterLink></li>
      <li v-if="authed" class="notif-link">
        <RouterLink to="/notifications">
          Notifications
          <span v-if="unreadCount > 0" class="unread-dot">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </RouterLink>
      </li>
    </ul>
    <div class="auth">
      <template v-if="authed">
        <span class="muted">Signed in as {{ displayName }}</span>
        <button class="btn-link" @click="onLogout">Logout</button>
      </template>
      <template v-else>
        <RouterLink to="/login">Login</RouterLink>
        <RouterLink to="/signup">Sign up</RouterLink>
      </template>
    </div>
  </nav>
</template>

<script>
import { RouterLink } from 'vue-router';
import { api } from '../api/client.js';
import {
  authStateVersion,
  clearCurrentUser,
  clearToken,
  getCurrentUser,
  isAuthenticated,
  readClaims,
  readRole
} from '../auth/token.js';
import { useCart } from '../composables/useCart.js';

const POLL_MS = 10000;

export default {
  name: 'AppNav',
  components: { RouterLink },
  setup() {
    // Expose the shared cart's itemCount as a reactive computed so the
    // navbar badge updates the moment an item is added or removed from
    // anywhere in the app.
    const { itemCount } = useCart();
    return { cartCount: itemCount };
  },
  data() {
    return {
      unreadCount: 0,
      pollTimer: null
    };
  },
  computed: {
    authed() {
      // Touch the shared auth version ref so updates from login/logout become reactive.
      // eslint-disable-next-line no-unused-expressions
      authStateVersion.value;
      return isAuthenticated();
    },
    isOwner() {
      // eslint-disable-next-line no-unused-expressions
      authStateVersion.value;
      const role = readRole();
      return role === 'RESTAURANT_OWNER' || role === 'ADMIN';
    },
    isDriver() {
      // eslint-disable-next-line no-unused-expressions
      authStateVersion.value;
      return readRole() === 'DRIVER';
    },
    displayName() {
      // Keep the label in sync when the current-user payload changes.
      // eslint-disable-next-line no-unused-expressions
      authStateVersion.value;
      const currentUser = getCurrentUser();
      if (currentUser) {
        return currentUser.fullName || currentUser.email || currentUser.userId || 'user';
      }
      const claims = readClaims();
      if (!claims) return 'user';
      return claims.sub || claims.userId || 'user';
    }
  },
  watch: {
    authed: {
      immediate: true,
      handler(isAuthed) {
        this.stopPolling();
        if (isAuthed) {
          this.fetchUnread();
          this.pollTimer = window.setInterval(this.fetchUnread, POLL_MS);
        } else {
          this.unreadCount = 0;
        }
      }
    }
  },
  beforeUnmount() {
    this.stopPolling();
  },
  methods: {
    stopPolling() {
      if (this.pollTimer) {
        window.clearInterval(this.pollTimer);
        this.pollTimer = null;
      }
    },
    async fetchUnread() {
      try {
        // The bearer token identifies the user; no header propagation needed.
        const res = await api.get('/api/notifications/unread-count');
        this.unreadCount = (res && res.unreadCount) || 0;
      } catch (_err) {
        // Silent: nav badge shouldn't surface errors. Keep previous count.
      }
    },
    onLogout() {
      clearToken();
      clearCurrentUser();
      this.$router.push({ name: 'login' });
    }
  }
};
</script>

<style scoped>
.app-nav {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1.25rem;
  background: #ffffff;
  border-bottom: 1px solid var(--qb-border);
}

.brand a {
  font-weight: 700;
  font-size: 1.15rem;
  color: var(--qb-accent);
  text-decoration: none;
}

.links {
  display: flex;
  gap: 1rem;
  list-style: none;
  margin: 0;
  padding: 0;
}

.links a, .auth a {
  color: var(--qb-fg);
  text-decoration: none;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
}

.unread-dot,
.cart-dot {
  display: inline-block;
  color: #fff;
  font-size: 0.7rem;
  font-weight: 700;
  border-radius: 999px;
  padding: 0 0.45rem;
  margin-left: 0.4rem;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  text-align: center;
  vertical-align: middle;
}

.unread-dot { background: #d9381e; }
.cart-dot { background: var(--qb-accent); }


.auth {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.btn-link {
  background: transparent;
  color: var(--qb-accent);
  padding: 0.25rem 0.5rem;
}
</style>
