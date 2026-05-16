<template>
  <section class="cart">
    <header class="list-header">
      <h1>Your cart</h1>
      <p v-if="cart.restaurantId" class="muted">
        From
        <router-link :to="{ name: 'restaurant-menu', params: { id: cart.restaurantId } }">
          {{ cart.restaurantName || 'restaurant' }}
        </router-link>
      </p>
    </header>

    <div v-if="error" class="error-banner">{{ error }}</div>

    <div v-if="cart.items.length === 0" class="empty">
      Your cart is empty.
      <router-link to="/restaurants" class="btn-link">Browse restaurants</router-link>
    </div>

    <ul v-else class="lines">
      <li v-for="line in cart.items" :key="line.menuItemId" class="line">
        <div class="line-head">
          <h3>{{ line.name }}</h3>
          <span class="muted">{{ line.unitPrice.toFixed(2) }} {{ line.currency }} each</span>
        </div>
        <div class="line-controls">
          <button type="button" class="qty-btn" @click="setQuantity(line.menuItemId, line.quantity - 1)">−</button>
          <input
            class="qty-input"
            type="number"
            min="0"
            :value="line.quantity"
            @change="onQtyChange(line.menuItemId, $event)"
          />
          <button type="button" class="qty-btn" @click="setQuantity(line.menuItemId, line.quantity + 1)">+</button>
          <span class="line-total">{{ (line.unitPrice * line.quantity).toFixed(2) }} {{ line.currency }}</span>
          <button type="button" class="btn-link remove" @click="removeItem(line.menuItemId)">Remove</button>
        </div>
      </li>
    </ul>

    <div v-if="cart.items.length > 0" class="summary">
      <div class="address">
        <label for="delivery-address">Delivery address</label>
        <input
          id="delivery-address"
          v-model.trim="deliveryAddress"
          type="text"
          placeholder="Street, city, postal code"
          :disabled="placing"
        />
        <p v-if="usingProfileAddress" class="muted small">
          Using your saved address.
          <router-link :to="{ name: 'user-profile' }" class="btn-link">Change in profile</router-link>
        </p>
        <p v-else-if="!profileAddressText" class="muted small">
          Tip: add an address to <router-link :to="{ name: 'user-profile' }" class="btn-link">your profile</router-link> to skip this next time.
        </p>
      </div>
      <div class="totals">
        <span>Total</span>
        <strong>{{ subtotal.toFixed(2) }} {{ currency }}</strong>
      </div>
      <div class="actions">
        <button type="button" class="btn-link" :disabled="placing" @click="clear">Clear cart</button>
        <button type="button" class="btn" :disabled="placing || !canPlace" @click="placeOrder">
          {{ placing ? 'Placing order…' : 'Place order' }}
        </button>
      </div>
      <p v-if="!canPlace && !placing" class="muted small">
        {{ canSignedIn ? 'Enter a delivery address to place the order.' : 'Sign in to place orders.' }}
      </p>
    </div>
  </section>
</template>

<script>
import { api, ApiError } from '../api/client.js';
import { getCurrentUser } from '../auth/token.js';
import { useCart } from '../composables/useCart.js';

// user-service stores address as { street, city, postalCode, label, isDefault }
// (see UserView's payload). Flatten the three location parts into the single
// string the order-service contract expects on deliveryAddress.
function formatProfileAddress(addr) {
  if (!addr || typeof addr !== 'object') return '';
  const parts = [addr.street, addr.city, addr.postalCode]
    .map((p) => (p == null ? '' : String(p).trim()))
    .filter(Boolean);
  return parts.join(', ');
}

export default {
  name: 'CartView',
  setup() {
    const { cart, subtotal, currency, setQuantity, removeItem, clear } = useCart();
    return { cart, subtotal, currency, setQuantity, removeItem, clear };
  },
  data() {
    return {
      placing: false,
      error: '',
      deliveryAddress: '',
      // Snapshot of the profile address at mount so we can tell whether
      // `deliveryAddress` still matches the saved one (for the "Using your
      // saved address" hint) without re-reading localStorage on every render.
      profileAddressText: ''
    };
  },
  computed: {
    canSignedIn() {
      const user = getCurrentUser();
      return Boolean(user && user.userId);
    },
    canPlace() {
      return Boolean(
        this.canSignedIn
          && this.cart.items.length > 0
          && this.cart.restaurantId
          && this.deliveryAddress
      );
    },
    usingProfileAddress() {
      return Boolean(this.profileAddressText)
        && this.deliveryAddress === this.profileAddressText;
    }
  },
  created() {
    const user = getCurrentUser();
    const formatted = formatProfileAddress(user && user.address);
    if (formatted) {
      this.profileAddressText = formatted;
      this.deliveryAddress = formatted;
    }
  },
  methods: {
    onQtyChange(menuItemId, event) {
      this.setQuantity(menuItemId, event.target.value);
    },
    async placeOrder() {
      this.error = '';
      const user = getCurrentUser();
      if (!user || !user.userId) {
        this.error = 'You must be signed in to place an order.';
        return;
      }
      if (!this.deliveryAddress) {
        this.error = 'Enter a delivery address before placing the order.';
        return;
      }
      this.placing = true;
      try {
        // order-service derives the customer id from the JWT in the
        // Authorization header (attached by apiFetch automatically).
        // Body shape per OrderController: restaurantId + items[] +
        // deliveryAddress. The server validates items against menu-service
        // and computes totalAmount itself; client unitPrice is ignored.
        const body = {
          restaurantId: this.cart.restaurantId,
          deliveryAddress: this.deliveryAddress,
          items: this.cart.items.map((i) => ({
            menuItemId: i.menuItemId,
            name: i.name,
            unitPrice: i.unitPrice,
            quantity: i.quantity
          }))
        };
        const placed = await api.post('/api/orders', body);
        this.clear();
        this.$router.push({ name: 'orders', params: { id: String(placed.orderId) } });
      } catch (err) {
        this.error = err instanceof ApiError ? err.message : 'Could not place the order.';
      } finally {
        this.placing = false;
      }
    }
  }
};
</script>

<style scoped>
.cart { max-width: 720px; }

.list-header { margin-bottom: 1rem; }
.list-header h1 { margin: 0 0 0.25rem; }

.empty {
  border: 1px dashed var(--qb-border);
  padding: 1.25rem;
  border-radius: 6px;
  background: #fff;
  color: var(--qb-muted);
  text-align: center;
}
.empty .btn-link { margin-left: 0.5rem; }

.lines { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 0.5rem; }

.line {
  background: #fff;
  border: 1px solid var(--qb-border);
  border-radius: 6px;
  padding: 0.75rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.line-head { display: flex; align-items: baseline; justify-content: space-between; gap: 0.5rem; }
.line-head h3 { margin: 0; color: var(--qb-accent); font-size: 1rem; }

.line-controls {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.qty-btn {
  background: #f3f4f6;
  color: var(--qb-fg);
  border: 1px solid var(--qb-border);
  border-radius: 4px;
  width: 2rem;
  height: 2rem;
  padding: 0;
  cursor: pointer;
  font-size: 1rem;
}

.qty-input {
  width: 3.5rem;
  text-align: center;
  padding: 0.3rem;
}

.line-total {
  margin-left: auto;
  font-weight: 600;
}

.remove { color: #9b1c1c; }

.summary {
  margin-top: 1rem;
  padding: 1rem;
  background: #fff;
  border: 1px solid var(--qb-border);
  border-radius: 6px;
}

.address { display: flex; flex-direction: column; gap: 0.3rem; margin-bottom: 0.75rem; }
.address label { font-size: 0.85rem; color: var(--qb-muted); }
.address input {
  padding: 0.45rem 0.6rem;
  border: 1px solid var(--qb-border);
  border-radius: 4px;
  font: inherit;
}

.totals {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 1.1rem;
}

.actions { display: flex; gap: 0.75rem; margin-top: 0.75rem; justify-content: flex-end; }

.btn-link {
  background: transparent;
  color: var(--qb-accent);
  border: 0;
  padding: 0.5rem 0.6rem;
  cursor: pointer;
}

.small { font-size: 0.85rem; margin-top: 0.5rem; }
</style>
