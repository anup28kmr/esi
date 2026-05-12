import { computed, ref, watch } from 'vue';

const STORAGE_KEY = 'quickbite.cart';

const empty = () => ({ restaurantId: null, restaurantName: '', items: [] });

function loadFromStorage() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return empty();
    const parsed = JSON.parse(raw);
    if (!parsed || !Array.isArray(parsed.items)) return empty();
    return {
      restaurantId: parsed.restaurantId || null,
      restaurantName: parsed.restaurantName || '',
      items: parsed.items
    };
  } catch (_e) {
    return empty();
  }
}

// Single module-level cart shared across every component that calls useCart().
// localStorage is the source of truth; a watcher mirrors writes back so a page
// refresh keeps the cart intact.
const cart = ref(loadFromStorage());

watch(cart, (next) => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  } catch (_e) {
    // Quota / private mode -- accept lossy behavior rather than crashing the UI.
  }
}, { deep: true });

function addItem(menuItem, restaurantId, restaurantName) {
  // Switching restaurants resets the cart. Mixed-restaurant orders aren't
  // supported by the order-service (one restaurantId per order), and silently
  // dropping the prior cart would be worse than confirming.
  if (cart.value.restaurantId && cart.value.restaurantId !== restaurantId) {
    const confirmed = typeof window !== 'undefined'
      ? window.confirm(`Your cart has items from ${cart.value.restaurantName || 'another restaurant'}. Replace them with this item?`)
      : true;
    if (!confirmed) return false;
    cart.value = empty();
  }
  if (!cart.value.restaurantId) {
    cart.value.restaurantId = restaurantId;
    cart.value.restaurantName = restaurantName || '';
  }
  const existing = cart.value.items.find((i) => i.menuItemId === menuItem.menuItemId);
  if (existing) {
    existing.quantity += 1;
  } else {
    cart.value.items.push({
      menuItemId: menuItem.menuItemId,
      name: menuItem.name,
      unitPrice: Number(menuItem.priceAmount),
      currency: menuItem.priceCurrency || 'EUR',
      quantity: 1
    });
  }
  return true;
}

function setQuantity(menuItemId, qty) {
  const q = Math.max(0, Math.floor(Number(qty) || 0));
  const idx = cart.value.items.findIndex((i) => i.menuItemId === menuItemId);
  if (idx === -1) return;
  if (q === 0) {
    cart.value.items.splice(idx, 1);
    if (cart.value.items.length === 0) clear();
    return;
  }
  cart.value.items[idx].quantity = q;
}

function removeItem(menuItemId) {
  setQuantity(menuItemId, 0);
}

function clear() {
  cart.value = empty();
}

const itemCount = computed(() => cart.value.items.reduce((n, i) => n + i.quantity, 0));
const subtotal = computed(() => cart.value.items.reduce((s, i) => s + i.unitPrice * i.quantity, 0));
const currency = computed(() => cart.value.items[0]?.currency || 'EUR');

export function useCart() {
  return { cart, itemCount, subtotal, currency, addItem, setQuantity, removeItem, clear };
}
