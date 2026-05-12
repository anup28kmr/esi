<template>
  <section class="order-status">
    <header class="list-header">
      <h1>{{ pageTitle }}</h1>
      <button type="button" class="btn-link" :disabled="loading" @click="load">
        {{ loading ? 'Refreshing…' : 'Refresh' }}
      </button>
    </header>

    <div v-if="error" class="error-banner">{{ error }}</div>

    <div v-if="loading && !order && orders.length === 0" class="muted" role="status">Loading…</div>

    <!-- DETAIL MODE: /orders/:id -->
    <article v-if="id && order" class="card">
      <div class="card-head">
        <div>
          <h2>Order #{{ order.orderId }}</h2>
          <p class="muted small">From {{ restaurantName(order.restaurantId) }}</p>
        </div>
        <span :class="['badge', statusClass(order.status)]">{{ order.status }}</span>
      </div>

      <ul v-if="order.items && order.items.length" class="lines">
        <li v-for="line in order.items" :key="line.menuItemId" class="line">
          <span class="line-name">{{ line.name }}</span>
          <span class="muted">{{ Number(line.unitPrice).toFixed(2) }} × {{ line.quantity }}</span>
          <span class="line-total">{{ (Number(line.unitPrice) * line.quantity).toFixed(2) }}</span>
        </li>
      </ul>

      <div class="totals">
        <span>Total</span>
        <strong>{{ Number(order.totalAmount).toFixed(2) }}</strong>
      </div>

      <div v-if="canCancel(order)" class="card-actions">
        <button
          type="button"
          class="cancel-btn"
          :disabled="cancellingId === order.orderId"
          @click="cancelOrder(order)"
        >
          {{ cancellingId === order.orderId ? 'Cancelling…' : 'Cancel order' }}
        </button>
      </div>
    </article>

    <!-- LIST MODE: /orders -->
    <template v-else-if="!id">
      <p v-if="!isLoggedIn" class="muted">Sign in to see your orders.</p>
      <div v-else-if="!loading && orders.length === 0" class="empty">
        <template v-if="isOwner">
          No orders have been placed at your restaurants yet.
        </template>
        <template v-else>
          You have no orders yet.
          <router-link to="/restaurants" class="btn-link">Browse restaurants</router-link>
        </template>
      </div>
      <ul v-else class="order-list">
        <li v-for="o in orders" :key="o.orderId" class="card order-row">
          <div class="card-head">
            <div>
              <h3>Order #{{ o.orderId }}</h3>
              <p class="muted small">
                From {{ restaurantName(o.restaurantId) }}
                · {{ (o.items || []).length }} item<span v-if="(o.items || []).length !== 1">s</span>
              </p>
            </div>
            <span :class="['badge', statusClass(o.status)]">{{ o.status }}</span>
          </div>
          <div class="order-row-foot">
            <strong>{{ Number(o.totalAmount || 0).toFixed(2) }}</strong>
            <div class="row-actions">
              <button
                v-if="canCancel(o)"
                type="button"
                class="cancel-btn"
                :disabled="cancellingId === o.orderId"
                @click="cancelOrder(o)"
              >
                {{ cancellingId === o.orderId ? 'Cancelling…' : 'Cancel' }}
              </button>
              <router-link :to="{ name: 'orders', params: { id: String(o.orderId) } }" class="btn-link">
                View details
              </router-link>
            </div>
          </div>
        </li>
      </ul>
    </template>

    <p class="muted back">
      <router-link v-if="id" :to="{ name: 'orders' }">← Back to your orders</router-link>
      <router-link v-else to="/restaurants">← Back to restaurants</router-link>
    </p>
  </section>
</template>

<script>
import { api, ApiError } from '../api/client.js';
import { getCurrentUser, isAuthenticated, readRole } from '../auth/token.js';

// user-service issues userId claims as new UUID(0L, longId).toString(), e.g.
// userId=2 -> "00000000-0000-0000-0000-000000000002". This mirrors that so
// the frontend can match a Long userId against a restaurant's ownerId UUID.
function longToOwnerUuid(longUserId) {
  if (longUserId === null || longUserId === undefined) return null;
  const n = Number(longUserId);
  if (!Number.isFinite(n) || n < 0) return null;
  // User IDs in this app are well inside Number.MAX_SAFE_INTEGER, so no
  // BigInt is needed. Format mirrors Java `new UUID(0L, n).toString()`.
  const hex = n.toString(16).padStart(16, '0');
  return `00000000-0000-0000-${hex.slice(0, 4)}-${hex.slice(4)}`;
}

export default {
  name: 'OrderStatusView',
  props: { id: { type: String, default: '' } },
  data() {
    return { order: null, orders: [], loading: false, error: '', restaurants: {}, cancellingId: null };
  },
  computed: {
    isLoggedIn() { return isAuthenticated(); },
    isOwner() {
      const role = readRole();
      return role === 'RESTAURANT_OWNER' || role === 'ADMIN';
    },
    pageTitle() {
      if (this.id) return 'Order';
      return this.isOwner ? 'Orders at your restaurants' : 'Your orders';
    }
  },
  watch: {
    id: { immediate: true, handler() { this.load(); } }
  },
  methods: {
    async load() {
      this.error = '';
      this.loading = true;
      try {
        if (this.id) {
          this.order = await api.get(`/api/orders/${this.id}`);
          await this.resolveRestaurants([this.order.restaurantId]);
        } else {
          const user = getCurrentUser();
          if (!user || !user.userId) {
            this.orders = [];
            return;
          }
          const list = this.isOwner
            ? await this.loadOwnerOrders(user.userId)
            : await this.loadCustomerOrders(user.userId);
          this.orders = list.sort((a, b) => Number(b.orderId) - Number(a.orderId));
          await this.resolveRestaurants(this.orders.map((o) => o.restaurantId));
        }
      } catch (err) {
        this.error = err instanceof ApiError ? err.message : 'Could not load orders.';
      } finally {
        this.loading = false;
      }
    },
    // order-service: GET /orders?customerId=<Long> returns this customer's orders.
    async loadCustomerOrders(userId) {
      const list = await api.get(`/api/orders?customerId=${encodeURIComponent(userId)}`);
      return Array.isArray(list) ? list : [];
    },
    // Owner view: look up restaurants whose ownerId matches the logged-in
    // user's synthesized UUID, then aggregate orders per restaurant.
    // restaurant-service has no ?ownerId= filter, so we page through and
    // filter client-side (small list in this app).
    async loadOwnerOrders(userId) {
      const ownerUuid = longToOwnerUuid(userId);
      if (!ownerUuid) return [];
      const page = await api.get('/api/restaurants?size=200');
      const all = Array.isArray(page?.content) ? page.content : (Array.isArray(page) ? page : []);
      const owned = all.filter((r) => r.ownerId === ownerUuid);
      if (owned.length === 0) return [];
      const results = await Promise.allSettled(
        owned.map((r) => api.get(`/api/orders?restaurantId=${encodeURIComponent(r.restaurantId)}`))
      );
      return results.flatMap((res) => (res.status === 'fulfilled' && Array.isArray(res.value)) ? res.value : []);
    },
    // Order responses only carry restaurantId. Fetch each referenced
    // restaurant once in parallel (deduped), cache by id, and fall back
    // to the raw id if the lookup fails so the UI still renders.
    async resolveRestaurants(ids) {
      const toFetch = [...new Set(ids.filter((id) => id && !(id in this.restaurants)))];
      if (toFetch.length === 0) return;
      const results = await Promise.allSettled(
        toFetch.map((id) => api.get(`/api/restaurants/${id}`))
      );
      const next = { ...this.restaurants };
      toFetch.forEach((id, i) => {
        const r = results[i];
        next[id] = r.status === 'fulfilled' ? (r.value?.name || id) : id;
      });
      this.restaurants = next;
    },
    restaurantName(id) {
      return this.restaurants[id] || id || '—';
    },
    statusClass(status) {
      const s = (status || '').toUpperCase();
      if (s.includes('DELIVER') || s.includes('COMPLET')) return 'success';
      if (s.includes('CANCEL') || s.includes('FAIL') || s.includes('REJECT')) return 'failure';
      return 'pending';
    },
    // order-service: DELETE /orders/{id} only succeeds while status is PENDING
    // (before the restaurant accepts). Customers are the only role who should
    // cancel their own orders -- owners managing restaurants have a different
    // workflow.
    canCancel(o) {
      return Boolean(o && !this.isOwner && (o.status || '').toUpperCase() === 'PENDING');
    },
    async cancelOrder(o) {
      if (!o || this.cancellingId !== null) return;
      const confirmed = typeof window !== 'undefined'
        ? window.confirm(`Cancel order #${o.orderId}? This can't be undone.`)
        : true;
      if (!confirmed) return;
      this.cancellingId = o.orderId;
      this.error = '';
      try {
        await api.delete(`/api/orders/${o.orderId}`);
        // Mark cancelled locally so the badge updates immediately, without
        // forcing a full refetch of all owner restaurants.
        if (this.order && this.order.orderId === o.orderId) {
          this.order = { ...this.order, status: 'CANCELLED' };
        }
        const idx = this.orders.findIndex((x) => x.orderId === o.orderId);
        if (idx !== -1) {
          this.orders.splice(idx, 1, { ...this.orders[idx], status: 'CANCELLED' });
        }
      } catch (err) {
        this.error = err instanceof ApiError
          ? `Could not cancel order #${o.orderId}: ${err.message}`
          : `Could not cancel order #${o.orderId}.`;
      } finally {
        this.cancellingId = null;
      }
    }
  }
};
</script>

<style scoped>
.order-status { max-width: 720px; }

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.75rem;
}
.list-header h1 { margin: 0; }

.empty {
  border: 1px dashed var(--qb-border);
  padding: 1.25rem;
  border-radius: 6px;
  background: #fff;
  color: var(--qb-muted);
  text-align: center;
}
.empty .btn-link { margin-left: 0.5rem; }

.card {
  background: #fff;
  border: 1px solid var(--qb-border);
  border-radius: 6px;
  padding: 1rem 1.25rem;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.5rem;
}

.card-head h2, .card-head h3 { margin: 0; color: var(--qb-accent); }
.card-head h3 { font-size: 1.05rem; }
.small { font-size: 0.85rem; margin: 0.2rem 0 0; }

.badge {
  font-size: 0.8rem;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-weight: 600;
  white-space: nowrap;
}
.badge.pending  { background: #fef3c7; color: #92400e; }
.badge.success  { background: #dcfce7; color: #166534; }
.badge.failure  { background: #fee2e2; color: #991b1b; }

.lines {
  list-style: none;
  margin: 0.75rem 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.line {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 0.75rem;
  align-items: baseline;
  padding: 0.4rem 0;
  border-bottom: 1px dashed var(--qb-border);
}
.line:last-child { border-bottom: 0; }
.line-name { font-weight: 600; }
.line-total { font-weight: 600; min-width: 4rem; text-align: right; }

.totals {
  display: flex;
  justify-content: space-between;
  font-size: 1.1rem;
  margin-top: 0.5rem;
  padding-top: 0.5rem;
  border-top: 1px solid var(--qb-border);
}

.order-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 0.6rem; }
.order-row-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.5rem;
}
.row-actions { display: flex; align-items: center; gap: 0.5rem; }

.card-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px dashed var(--qb-border);
}

.cancel-btn {
  background: transparent;
  color: #9b1c1c;
  border: 1px solid #f8b4b4;
  border-radius: 4px;
  padding: 0.3rem 0.7rem;
  font-size: 0.85rem;
  cursor: pointer;
}
.cancel-btn:hover:not(:disabled) { background: #fde8e8; }
.cancel-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.btn-link {
  background: transparent;
  color: var(--qb-accent);
  border: 0;
  padding: 0.4rem 0.6rem;
  cursor: pointer;
  font-size: 0.95rem;
  text-decoration: none;
}
.btn-link:disabled { opacity: 0.6; cursor: not-allowed; }

.back { margin-top: 1rem; }
.back a { color: var(--qb-muted); text-decoration: none; }
</style>
