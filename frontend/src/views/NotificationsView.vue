<template>
  <section class="notifications">
    <h1>My notifications</h1>
    <p class="muted">
      Reads from <code>notification-service</code> through the API gateway. The
      <code>X-User-Id</code> header would normally be set by the gateway after
      token validation; until that lands in CP3, this page asks for the id.
    </p>

    <form class="lookup" @submit.prevent="load">
      <label>
        User ID (UUID)
        <input v-model.trim="userId" placeholder="11111111-1111-1111-1111-111111111111" required />
      </label>
      <button type="submit" :disabled="loading">Load</button>
    </form>

    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="loaded" class="summary">
      <div class="card">
        <div class="card-label">Unread</div>
        <div class="card-value">{{ unreadCount }}</div>
      </div>
      <div class="card">
        <div class="card-label">Total visible</div>
        <div class="card-value">{{ items.length }}</div>
      </div>
      <button class="btn-link" :disabled="!unreadCount" @click="markAllRead">
        Mark all as read
      </button>
    </div>

    <ul v-if="loaded && items.length" class="list">
      <li v-for="item in items" :key="item.id" :class="['row', item.status.toLowerCase()]">
        <div class="row-main">
          <span class="channel">{{ item.channel }}</span>
          <span class="message">{{ item.message }}</span>
        </div>
        <div class="row-meta">
          <span class="status">{{ item.status }}</span>
          <span v-if="item.sentAt" class="ts">{{ formatTs(item.sentAt) }}</span>
        </div>
      </li>
    </ul>

    <p v-else-if="loaded" class="muted">No notifications for this user yet.</p>
  </section>
</template>

<script>
import { api, ApiError } from '../api/client.js';

export default {
  name: 'NotificationsView',
  data() {
    return {
      userId: '11111111-1111-1111-1111-111111111111',
      items: [],
      unreadCount: 0,
      loading: false,
      loaded: false,
      error: ''
    };
  },
  methods: {
    async load() {
      this.loading = true;
      this.error = '';
      try {
        const headers = { 'X-User-Id': this.userId };
        const [count, list] = await Promise.all([
          api.get('/notifications/unread-count', { headers }),
          api.get('/notifications', { headers })
        ]);
        this.unreadCount = (count && count.unreadCount) || 0;
        this.items = Array.isArray(list) ? list : [];
        this.loaded = true;
      } catch (err) {
        this.error =
          err instanceof ApiError
            ? `${err.status || ''} ${err.message}`.trim()
            : err.message || 'Unknown error';
      } finally {
        this.loading = false;
      }
    },
    async markAllRead() {
      try {
        await api.patch('/notifications/read-all', null, {
          headers: { 'X-User-Id': this.userId }
        });
        await this.load();
      } catch (err) {
        this.error = err.message || 'Failed to mark all as read';
      }
    },
    formatTs(iso) {
      try {
        return new Date(iso).toLocaleString();
      } catch (_e) {
        return iso;
      }
    }
  }
};
</script>

<style scoped>
.notifications {
  max-width: 720px;
  margin: 0 auto;
  padding: 1.5rem 1rem;
}
.lookup {
  display: flex;
  gap: 0.75rem;
  align-items: end;
  margin: 1rem 0;
}
.lookup label {
  display: flex;
  flex-direction: column;
  flex: 1;
  font-size: 0.875rem;
}
.lookup input {
  padding: 0.5rem;
  border: 1px solid var(--qb-border);
  border-radius: 4px;
  font-family: inherit;
}
.summary {
  display: flex;
  gap: 1rem;
  align-items: center;
  margin: 1rem 0;
}
.card {
  border: 1px solid var(--qb-border);
  border-radius: 6px;
  padding: 0.75rem 1rem;
  background: #fff;
}
.card-label {
  font-size: 0.75rem;
  color: var(--qb-muted);
  text-transform: uppercase;
}
.card-value {
  font-size: 1.5rem;
  font-weight: 700;
}
.list {
  list-style: none;
  padding: 0;
  margin: 1rem 0;
}
.row {
  display: flex;
  justify-content: space-between;
  border: 1px solid var(--qb-border);
  border-radius: 6px;
  padding: 0.75rem;
  margin-bottom: 0.5rem;
  background: #fff;
}
.row.read {
  opacity: 0.6;
}
.row-main {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}
.channel {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--qb-accent);
}
.row-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  font-size: 0.75rem;
  color: var(--qb-muted);
}
.status {
  font-weight: 600;
}
.error {
  color: #b00020;
  margin: 0.5rem 0;
}
.muted {
  color: var(--qb-muted);
  font-size: 0.875rem;
}
</style>
