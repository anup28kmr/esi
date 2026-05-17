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
    <div v-if="claimError" class="error-banner">{{ claimError }}</div>

    <div v-if="loading" class="muted" role="status">Loading deliveries…</div>

    <div v-else-if="deliveries.length === 0" class="empty">
      No deliveries found.
    </div>

    <div v-else class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Pickup Address</th>
            <th>Delivery Address</th>
            <th>Status</th>
            <th v-if="isDriver">Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="d in deliveries" :key="d.deliveryId">
            <td class="id-cell">{{ shortId(d.deliveryId) }}</td>
            <td>{{ d.pickupAddress }}</td>
            <td>{{ d.deliveryAddress }}</td>
            <td>
              <span :class="['badge', statusClass(d.status)]">{{ d.status }}</span>
            </td>
            <td v-if="isDriver">
              <button
                v-if="d.status === 'PENDING' && !d.driverId"
                class="btn-take"
                :disabled="claiming === d.deliveryId"
                @click="claim(d.deliveryId)"
              >
                {{ claiming === d.deliveryId ? 'Taking…' : 'Take' }}
              </button>
              <span v-else-if="d.driverId" class="taken-label">Taken</span>
              <span v-else class="muted">—</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script>
import { api, ApiError } from '../api/client.js';
import { readRole } from '../auth/token.js';

export default {
  name: 'DeliveriesView',
  data() {
    return {
      deliveries: [],
      loading: false,
      error: '',
      claimError: '',
      statusFilter: '',
      claiming: null
    };
  },
  computed: {
    isDriver() {
      return readRole() === 'DRIVER';
    }
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
    async claim(deliveryId) {
      this.claimError = '';
      this.claiming = deliveryId;
      try {
        const updated = await api.post(`/api/deliveries/${deliveryId}/claim`);
        const idx = this.deliveries.findIndex(d => d.deliveryId === deliveryId);
        if (idx !== -1) this.deliveries[idx] = updated;
      } catch (err) {
        this.claimError = err instanceof ApiError ? err.message : 'Could not claim delivery.';
      } finally {
        this.claiming = null;
      }
    },
    reset() {
      this.statusFilter = '';
      this.load();
    },
    shortId(uuid) {
      return uuid ? uuid.slice(0, 8) + '…' : '—';
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

.table-wrap {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border: 1px solid var(--qb-border);
  border-radius: 6px;
  overflow: hidden;
}

thead {
  background: #f8fafc;
}

th {
  text-align: left;
  padding: 0.65rem 1rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--qb-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 1px solid var(--qb-border);
}

td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--qb-border);
  font-size: 0.9rem;
  vertical-align: middle;
}

tbody tr:last-child td {
  border-bottom: none;
}

tbody tr:hover {
  background: #f8fafc;
}

.id-cell {
  font-weight: 600;
  color: var(--qb-accent);
  font-size: 0.85rem;
  white-space: nowrap;
}

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

.btn-take {
  padding: 0.3rem 0.85rem;
  background: var(--qb-accent);
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.85rem;
  font-weight: 600;
}
.btn-take:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.taken-label {
  font-size: 0.8rem;
  color: var(--qb-muted);
  font-style: italic;
}

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
