<template>
  <section class="deliveries">
    <header class="list-header">
      <h1>Deliveries</h1>
    </header>

    <form class="filters" @submit.prevent="load">
      <div class="field">
        <label for="filter-status">Status</label>
        <select id="filter-status" v-model="statusFilter">
          <option value="">All</option>
          <option value="PENDING">Pending</option>
          <option value="ASSIGNED">Assigned</option>
          <option value="PICKED_UP">Picked up</option>
          <option value="IN_TRANSIT">In transit</option>
          <option value="DELIVERED">Delivered</option>
          <option value="FAILED">Failed</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
      </div>
      <div class="field actions">
        <button type="submit" :disabled="loading">Apply</button>
        <button type="button" class="btn-link" @click="reset" :disabled="loading">Reset</button>
      </div>
    </form>

    <div v-if="error" class="error-banner">{{ error }}</div>

    <div v-if="loading" class="muted" role="status">Loading deliveries…</div>

    <div v-else-if="deliveries.length === 0" class="empty">
      No deliveries found.
    </div>

    <ul v-else class="cards">
      <li v-for="d in deliveries" :key="d.deliveryId" class="card">
        <div class="card-head">
          <span class="delivery-id">{{ shortId(d.deliveryId) }}</span>
          <span :class="['badge', statusClass(d.status)]">{{ d.status }}</span>
        </div>
        <p class="muted addr">
          <strong>From:</strong> {{ d.pickupAddress }}
        </p>
        <p class="muted addr">
          <strong>To:</strong> {{ d.deliveryAddress }}
        </p>
        <p v-if="d.driverId" class="muted">
          Driver: {{ shortId(d.driverId) }}
        </p>
        <p v-if="d.actualDeliveryTime" class="muted">
          Delivered: {{ formatTime(d.actualDeliveryTime) }}
        </p>
        <p class="muted created">
          Created: {{ formatTime(d.createdAt) }}
        </p>
      </li>
    </ul>
  </section>
</template>

<script>
import { api, ApiError } from '../api/client.js';

export default {
  name: 'DeliveriesView',
  data() {
    return {
      deliveries: [],
      loading: false,
      error: '',
      statusFilter: ''
    };
  },
  mounted() {
    this.load();
  },
  methods: {
    async load() {
      this.loading = true;
      this.error = '';
      try {
        const path = this.statusFilter
          ? `/api/deliveries?status=${this.statusFilter}`
          : '/api/deliveries';
        const data = await api.get(path);
        this.deliveries = Array.isArray(data) ? data : [];
      } catch (err) {
        this.error = err instanceof ApiError ? err.message : 'Could not load deliveries.';
        this.deliveries = [];
      } finally {
        this.loading = false;
      }
    },
    reset() {
      this.statusFilter = '';
      this.load();
    },
    shortId(uuid) {
      return uuid ? uuid.slice(0, 8) + '…' : '—';
    },
    formatTime(ts) {
      if (!ts) return '—';
      return new Date(ts).toLocaleString();
    },
    statusClass(status) {
      const map = {
        PENDING: 'pending',
        ASSIGNED: 'assigned',
        PICKED_UP: 'transit',
        IN_TRANSIT: 'transit',
        DELIVERED: 'delivered',
        FAILED: 'failed',
        CANCELLED: 'failed'
      };
      return map[status] || '';
    }
  }
};
</script>

<style scoped>
.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.75rem;
}
.list-header h1 { margin: 0; }

.filters {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0.75rem;
  align-items: end;
  padding: 0.75rem;
  background: #fff;
  border: 1px solid var(--qb-border);
  border-radius: 6px;
  margin-bottom: 1rem;
}
.filters .field { display: flex; flex-direction: column; }
.filters .actions { flex-direction: row; gap: 0.5rem; align-items: center; }

.empty {
  border: 1px dashed var(--qb-border);
  padding: 1.25rem;
  border-radius: 6px;
  background: #fff;
  color: var(--qb-muted);
  text-align: center;
}

.cards {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
}

.card {
  background: #fff;
  border: 1px solid var(--qb-border);
  border-radius: 6px;
  padding: 1rem;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.delivery-id {
  font-weight: 600;
  color: var(--qb-accent);
  font-size: 0.9rem;
}

.addr { font-size: 0.9rem; margin: 0.2rem 0; }
.created { font-size: 0.8rem; margin-top: 0.5rem; }

.badge {
  font-size: 0.72rem;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  font-weight: 600;
  white-space: nowrap;
}
.badge.pending   { background: #fef9c3; color: #854d0e; }
.badge.assigned  { background: #dbeafe; color: #1e40af; }
.badge.transit   { background: #ffedd5; color: #9a3412; }
.badge.delivered { background: #dcfce7; color: #166534; }
.badge.failed    { background: #fee2e2; color: #991b1b; }

.btn-link {
  background: transparent;
  color: var(--qb-accent);
  padding: 0.25rem 0;
  border: 0;
  cursor: pointer;
}

.error-banner {
  padding: 0.75rem 1rem;
  background: #fee2e2;
  color: #991b1b;
  border-radius: 6px;
  margin-bottom: 1rem;
}
</style>
