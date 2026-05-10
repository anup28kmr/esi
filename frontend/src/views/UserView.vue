<template>
  <section class="user-profile">
  <header class="page-header">
    <div>
    <h1>User Profile</h1>
    <p class="muted">
      View your account details and update your profile through
      <code>PUT /api/users/:id</code>.
    </p>
    </div>
    <button class="btn-secondary" type="button" @click="loadProfile" :disabled="loading || saving">
    Refresh
    </button>
  </header>

  <div v-if="loading" class="info-banner">Loading your profile…</div>
  <div v-else-if="error" class="error-banner">{{ error }}</div>

  <template v-else>
    <section class="summary-card">
    <h2>Account details</h2>
    <dl class="summary-grid">
      <div>
      <dt>User ID</dt>
      <dd>{{ profile.userId }}</dd>
      </div>
      <div>
      <dt>Email</dt>
      <dd>{{ form.email }}</dd>
      </div>
      <div>
      <dt>Role</dt>
      <dd>{{ form.role }}</dd>
      </div>
      <div>
      <dt>Status</dt>
      <dd>{{ form.status }}</dd>
      </div>
      <div class="full-width" v-if="addressText">
      <dt>Address</dt>
      <dd>{{ addressText }}</dd>
      </div>
      <div class="full-width" v-else>
      <dt>Address</dt>
      <dd class="muted">No saved address</dd>
      </div>
    </dl>
    </section>

    <section class="edit-card">
    <h2>Update profile</h2>
    <p class="muted">
      Update your name, phone number, and password. Leave the password field blank to keep your current one.
    </p>

    <form @submit.prevent="onSubmit">
      <div class="form-grid">
      <label>
        <span>User ID</span>
        <input type="text" :value="form.userId" readonly />
      </label>

      <label>
        <span>Email</span>
        <input type="email" :value="form.email" readonly />
      </label>

      <label>
        <span>Full name</span>
        <input v-model="form.fullName" type="text" required />
      </label>

      <label>
        <span>Phone number</span>
        <input v-model="form.phoneNumber" type="text" />
      </label>

      <label>
        <span>Role</span>
        <input type="text" :value="form.role" readonly />
      </label>

      <label>
        <span>Status</span>
        <input type="text" :value="form.status" readonly />
      </label>
        <label>
          <span>Password</span>
          <input v-model="form.password" type="password" autocomplete="new-password" />
        </label>
        <label>
        <span>Street</span>
        <input type="text" :value="address.street"  />
        </label>
        <label>
        <span>City</span>
        <input type="text" :value="address.city"  />
        </label>
        <label>
        <span>Postal code</span>
        <input type="text" :value="address.postalCode"  />
        </label>
      </div>


      <div v-if="success" class="success-banner">{{ success }}</div>
      <div v-if="formError" class="error-banner">{{ formError }}</div>

      <div class="actions">
      <button type="submit" :disabled="saving">
        {{ saving ? 'Saving…' : 'Save changes' }}
      </button>
      </div>
    </form>
    </section>
  </template>
  </section>
</template>

<script>
/* eslint-disable vue/no-unused-properties */
import { api, ApiError } from '../api/client.js';
import { getCurrentUser, setCurrentUser } from '../auth/token.js';

export default {
  name: 'UserView',
  data() {
  return {
    loading: true,
    saving: false,
    error: '',
    formError: '',
    success: '',
    profile: null,
    form: {
    userId: null,
    email: '',
    password: '',
    fullName: '',
    phoneNumber: '',
    role: 'CUSTOMER',
    status: 'ACTIVE'
    },
    address: {
    street: '',
    city: '',
    postalCode: '',
    label: '',
    isDefault: false
    }
  };
  },
  computed: {
  addressText() {
    const parts = [this.address.street, this.address.city, this.address.postalCode].filter(Boolean);
    return parts.length ? parts.join(', ') : '';
  }
  },
  created() {
  this.loadProfile();
  },
  methods: {
  async loadProfile() {
    this.loading = true;
    this.error = '';
    this.formError = '';
    this.success = '';

    try {
    const cached = getCurrentUser();
    const userId = cached?.userId;
    if (!userId) {
      this.error = 'No current user profile is available. Please sign in again.';
      return;
    }

    const profile = await api.get(`/api/users/${userId}`);
    this.profile = profile;
    this.form.userId = profile.userId ?? userId;
    this.form.email = profile.email || '';
    this.form.password = '';
    this.form.fullName = profile.fullName || '';
    this.form.phoneNumber = profile.phoneNumber || '';
    this.form.role = profile.role || 'CUSTOMER';
    this.form.status = profile.status || 'ACTIVE';
    this.address = profile.address || {
      street: '',
      city: '',
      postalCode: '',
      label: '',
      isDefault: false
    };
    setCurrentUser(profile);
    } catch (err) {
    console.error('[UserView] loadProfile error:', err);
    this.error = err instanceof ApiError ? err.message : 'Failed to load profile.';
    } finally {
    this.loading = false;
    }
  },
  async onSubmit() {
    this.formError = '';
    this.success = '';
    this.saving = true;

    try {
    if (!this.form.userId) {
      this.formError = 'Missing user id.';
      return;
    }

    const payload = {
      userId: this.form.userId,
      email: this.form.email,
      fullName: this.form.fullName,
      phoneNumber: this.form.phoneNumber,
      role: this.form.role,
      status: this.form.status,
      address: this.address.street || this.address.city || this.address.postalCode ? { ...this.address } : null
    };

    if (this.form.password && this.form.password.trim()) {
      payload.password = this.form.password.trim();
    }

    console.log('[UserView] Sending PUT /api/users/%s with payload:', this.form.userId, payload);
    const updated = await api.put(`/api/users/${this.form.userId}`, payload);
    this.profile = updated;
    this.form.userId = updated.userId ?? this.form.userId;
    this.form.email = updated.email || this.form.email;
    this.form.password = '';
    this.form.fullName = updated.fullName || '';
    this.form.phoneNumber = updated.phoneNumber || '';
    this.form.role = updated.role || this.form.role;
    this.form.status = updated.status || this.form.status;
    this.address = updated.address || this.address;
    setCurrentUser(updated);
    this.success = 'Profile updated successfully.';
    } catch (err) {
    console.error('[UserView] update error:', err);
    this.formError = err instanceof ApiError ? err.message : 'Failed to update profile.';
    } finally {
    this.saving = false;
    }
  }
  }
};
</script>

<style scoped>
.user-profile {
  max-width: 920px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  margin-bottom: 1.5rem;
}

.summary-card,
.edit-card {
  background: #fff;
  border: 1px solid var(--qb-border);
  border-radius: 12px;
  padding: 1rem 1.25rem;
  margin-bottom: 1rem;
}

.summary-grid,
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.summary-grid dt {
  font-size: 0.85rem;
  color: var(--qb-muted);
}

.summary-grid dd {
  margin: 0.25rem 0 0;
  font-weight: 600;
}

.full-width {
  grid-column: 1 / -1;
}

label span {
  display: block;
  margin-bottom: 0.35rem;
  font-weight: 600;
}

input {
  width: 100%;
}

.advanced-fields {
  margin-top: 1rem;
}

.advanced-fields summary {
  cursor: pointer;
  font-weight: 600;
  margin-bottom: 0.75rem;
}

.compact-grid {
  margin-top: 0.75rem;
}

.actions {
  margin-top: 1rem;
  display: flex;
  justify-content: flex-end;
}

.btn-secondary {
  background: transparent;
  color: var(--qb-accent);
  border: 1px solid var(--qb-accent);
}

@media (max-width: 720px) {
  .summary-grid,
  .form-grid {
  grid-template-columns: 1fr;
  }

  .page-header {
  flex-direction: column;
  align-items: flex-start;
  }
}
</style>

