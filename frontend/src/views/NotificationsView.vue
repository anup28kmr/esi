<template>
  <section class="notifications">
    <header class="page-head">
      <h1>Notifications</h1>
      <div class="head-actions">
        <span v-if="unreadCount" class="badge">{{ unreadCount }} unread</span>
        <button class="btn-link" :disabled="!unreadCount" @click="markAllRead">
          Mark all as read
        </button>
      </div>
    </header>

    <div v-if="error" class="error">{{ error }}</div>

    <ul v-if="items.length" class="feed">
      <li
        v-for="item in items"
        :key="item.id"
        :class="['notif', isRead(item) ? 'read' : 'unread']"
      >
        <div class="icon" :title="item.eventType || item.channel">
          {{ iconFor(item) }}
        </div>
        <div class="body">
          <div class="title-row">
            <span class="title">{{ titleFor(item) }}</span>
            <span class="ts" :title="formatAbsolute(item.sentAt)">
              {{ formatRelative(item.sentAt) }}
            </span>
          </div>
          <div class="message">{{ item.message }}</div>
          <div class="meta">
            <span class="chip">{{ item.channel }}</span>
            <span class="chip status">{{ item.status }}</span>
            <button
              v-if="!isRead(item)"
              class="mark-one"
              @click="markOneRead(item)"
            >
              Mark as read
            </button>
          </div>
        </div>
      </li>
    </ul>

    <p v-else-if="loaded" class="empty">
      You're all caught up — no notifications yet.
    </p>
    <p v-else class="empty">Loading…</p>
  </section>
</template>

<script>
import { api, ApiError } from '../api/client.js';
import { getCurrentUser, readClaims } from '../auth/token.js';

const POLL_MS = 5000;

const EVENT_ICONS = {
  PAYMENT_CONFIRMED: '💳',
  PAYMENT_FAILED: '⚠️',
  PAYMENT_REFUNDED: '↩️',
  DELIVERY_DISPATCHED: '🚚',
  DELIVERY_ASSIGNED: '🚴',
  DELIVERY_COMPLETED: '✅',
  ORDER_PLACED: '🧾',
  ORDER_CONFIRMED: '👍',
  ORDER_CANCELLED: '❌'
};

const CHANNEL_ICONS = { PUSH: '🔔', EMAIL: '✉️', SMS: '💬' };

function resolveUserId() {
  const user = getCurrentUser();
  if (user && user.userId) return user.userId;
  const claims = readClaims();
  if (claims && claims.userId) return claims.userId;
  return null;
}

export default {
  name: 'NotificationsView',
  data() {
    return {
      items: [],
      unreadCount: 0,
      loaded: false,
      error: '',
      pollTimer: null
    };
  },
  mounted() {
    this.refresh();
    this.pollTimer = window.setInterval(this.refresh, POLL_MS);
  },
  beforeUnmount() {
    if (this.pollTimer) window.clearInterval(this.pollTimer);
  },
  methods: {
    async refresh() {
      const userId = resolveUserId();
      if (!userId) {
        // Router guard will redirect; don't surface an error in the meantime.
        return;
      }
      try {
        const headers = { 'X-User-Id': userId };
        const [count, list] = await Promise.all([
          api.get('/api/notifications/unread-count', { headers }),
          api.get('/api/notifications', { headers })
        ]);
        this.unreadCount = (count && count.unreadCount) || 0;
        this.items = Array.isArray(list) ? list : [];
        this.loaded = true;
        this.error = '';
      } catch (err) {
        // 401s redirect via the client; transient network blips shouldn't
        // wipe the screen, only first-load errors should be visible.
        if (err instanceof ApiError && err.status === 401) return;
        const msg = err instanceof ApiError
          ? `${err.status || ''} ${err.message}`.trim()
          : err.message || 'Unknown error';
        if (!this.loaded) this.error = msg;
      }
    },
    async markAllRead() {
      const userId = resolveUserId();
      if (!userId) return;
      try {
        await api.patch('/api/notifications/read-all', null, {
          headers: { 'X-User-Id': userId }
        });
        await this.refresh();
      } catch (err) {
        this.error = err.message || 'Failed to mark all as read';
      }
    },
    async markOneRead(item) {
      const userId = resolveUserId();
      if (!userId || !item || !item.id) return;
      try {
        await api.patch(`/api/notifications/${item.id}/read`, null, {
          headers: { 'X-User-Id': userId }
        });
        await this.refresh();
      } catch (err) {
        this.error = err.message || 'Failed to mark as read';
      }
    },
    isRead(item) {
      return item && item.status === 'READ';
    },
    iconFor(item) {
      if (!item) return '🔔';
      if (item.eventType && EVENT_ICONS[item.eventType]) return EVENT_ICONS[item.eventType];
      if (CHANNEL_ICONS[item.channel]) return CHANNEL_ICONS[item.channel];
      return '🔔';
    },
    titleFor(item) {
      if (item && item.eventType) {
        return item.eventType
          .toLowerCase()
          .split('_')
          .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
          .join(' ');
      }
      return 'Notification';
    },
    formatRelative(iso) {
      if (!iso) return '';
      const then = new Date(iso).getTime();
      if (Number.isNaN(then)) return '';
      const diffSec = Math.round((Date.now() - then) / 1000);
      if (diffSec < 5) return 'just now';
      if (diffSec < 60) return `${diffSec}s ago`;
      const min = Math.round(diffSec / 60);
      if (min < 60) return `${min} min ago`;
      const hr = Math.round(min / 60);
      if (hr < 24) return `${hr} hr ago`;
      const day = Math.round(hr / 24);
      if (day < 7) return `${day} d ago`;
      return new Date(iso).toLocaleDateString();
    },
    formatAbsolute(iso) {
      if (!iso) return '';
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

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.badge {
  background: var(--qb-accent);
  color: #fff;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
}

.feed {
  list-style: none;
  padding: 0;
  margin: 0;
}

.notif {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
  padding: 0.85rem 1rem;
  border: 1px solid var(--qb-border);
  border-radius: 8px;
  margin-bottom: 0.6rem;
  background: #fff;
  transition: background-color 0.15s ease;
}

.notif.unread {
  background: #fff7e6;
  border-left: 4px solid var(--qb-accent);
}

.notif.read {
  opacity: 0.65;
}

.icon {
  font-size: 1.5rem;
  line-height: 1;
  flex-shrink: 0;
  padding-top: 0.1rem;
}

.body {
  flex: 1;
  min-width: 0;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 0.5rem;
}

.title {
  font-weight: 600;
  color: var(--qb-fg);
}

.ts {
  font-size: 0.75rem;
  color: var(--qb-muted);
  flex-shrink: 0;
}

.message {
  margin-top: 0.2rem;
  color: var(--qb-fg);
  word-break: break-word;
}

.meta {
  margin-top: 0.4rem;
  display: flex;
  gap: 0.4rem;
  align-items: center;
}

.chip {
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
  padding: 0.1rem 0.5rem;
  border-radius: 999px;
  background: #f1f1f1;
  color: var(--qb-muted);
}

.chip.status {
  background: #eaf6ea;
  color: #1f7a1f;
}

.notif.unread .chip.status {
  background: #fff1d6;
  color: #8a5a00;
}

.mark-one {
  margin-left: auto;
  background: transparent;
  border: none;
  color: var(--qb-accent);
  cursor: pointer;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
  font-family: inherit;
}

.mark-one:hover {
  background: rgba(217, 56, 30, 0.08);
  text-decoration: underline;
}

.btn-link {
  background: transparent;
  color: var(--qb-accent);
  border: none;
  padding: 0.25rem 0.5rem;
  cursor: pointer;
  font: inherit;
}

.btn-link:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.error {
  color: #b00020;
  margin: 0.5rem 0;
  padding: 0.5rem;
  background: #fdecea;
  border-radius: 4px;
}

.empty {
  color: var(--qb-muted);
  text-align: center;
  padding: 2rem 0;
}
</style>
