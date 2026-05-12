<template>
  <nav class="app-nav">
    <div class="brand">
      <RouterLink to="/">QuickBite</RouterLink>
    </div>
    <ul class="links">
      <li><RouterLink to="/">Home</RouterLink></li>
      <li><RouterLink to="/restaurants">Restaurants</RouterLink></li>
      <li v-if="authed"><RouterLink to="/profile">User Profile</RouterLink></li>
      <li v-if="authed">
        <RouterLink to="/cart">
          Cart<span v-if="cartCount > 0" class="cart-badge">{{ cartCount }}</span>
        </RouterLink>
      </li>
      <li v-if="authed"><RouterLink to="/orders">Orders</RouterLink></li>
      <li><RouterLink to="/notifications">Notifications</RouterLink></li>
      <li><RouterLink to="/deliveries">Deliveries</RouterLink></li>
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
import { clearCurrentUser, clearToken, getCurrentUser, isAuthenticated, readClaims } from '../auth/token.js';
import { useCart } from '../composables/useCart.js';

export default {
  name: 'AppNav',
  components: { RouterLink },
  setup() {
    const { itemCount } = useCart();
    return { cartCount: itemCount };
  },
  computed: {
    authed() {
      return isAuthenticated();
    },
    displayName() {
      const currentUser = getCurrentUser();
      if (currentUser) {
        return currentUser.fullName || currentUser.email || currentUser.userId || 'user';
      }
      const claims = readClaims();
      if (!claims) return 'user';
      return claims.sub || claims.userId || 'user';
    }
  },
  methods: {
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

.cart-badge {
  display: inline-block;
  margin-left: 0.35rem;
  background: var(--qb-accent);
  color: #fff;
  font-size: 0.7rem;
  font-weight: 700;
  border-radius: 999px;
  padding: 0.05rem 0.4rem;
  vertical-align: top;
}
</style>
