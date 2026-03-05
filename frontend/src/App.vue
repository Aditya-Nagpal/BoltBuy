<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { ShoppingCart, Zap, CheckCircle, AlertCircle } from 'lucide-vue-next';
import { orderService } from './api/orderService';
import axios from 'axios';
import { v4 as uuidv4 } from 'uuid';

const stock = ref(0);
const loading = ref(false);
const status = ref(null); 
const message = ref('');
let poller = null;

const orders = ref([]);

const fetchRecentOrders = async () => {
    try {
        const response = await axios.get('/api/v1/worker/recent-orders');
        console.log("Fetched recent orders:", response.data);
        orders.value = response.data.map(o => ({
            userId: o.userId,
            time: new Date(o.createdAt).toLocaleTimeString()
        }));
    } catch (err) {
        console.error("Order fetch error:", err);
    }
};

const fetchStock = async () => {
    try {
        const response = await axios.get('/api/v1/orders/stock/1');
        stock.value = response.data;
        if (stock.value <= 0) {
            clearInterval(poller);
            status.value = 'error';
            message.value = "The flash sale has ended. All items sold out!";
        }
    } catch (err) {
        console.error("Poller error:", err);
    }
};

const currentIdempotencyKey = ref(uuidv4()); 

const handlePurchase = async () => {
  loading.value = true;
  status.value = null;
  
  try {
    // Pass the key to your service
    const data = await orderService.placeOrder(
        1, 
        "user_" + Math.floor(Math.random() * 1000),
        currentIdempotencyKey.value // Send the stored key
    );
    
    message.value = data;
    status.value = 'success';
    stock.value--; 

    // SUCCESS: Generate a fresh key for the NEXT potential purchase
    currentIdempotencyKey.value = uuidv4(); 
    
    } catch (err) {
        // Check if it's a 409 Conflict (Idempotency Hit)
        if(err.response?.status === 409) {
            message.value = "Order is already in progress. Checking status...";
            // Optional: You could trigger a fetchStock() here to see if it actually went through
        } else {
            message.value = err.response?.data?.message || "Flash sale busy!";
        }
        status.value = 'error';

        // NOTE: We do NOT rotate the UUID here. 
        // If the user clicks "BUY NOW" again, it sends the SAME UUID.
    } finally {
        loading.value = false;
    }
};

onMounted(() => {
    fetchStock();
    fetchRecentOrders();
    poller = setInterval(() => {
        fetchStock();
        fetchRecentOrders();
    }, 3000);
});

onUnmounted(() => {
    if(poller) clearInterval(poller);
});
</script>

<template>
    <div class="max-w-md w-full mx-auto p-4">
        <div class="bg-white rounded-2xl shadow-2xl overflow-hidden border border-gray-100">
            <div class="h-48 bg-black flex items-center justify-center">
                <Zap class="text-yellow-400 w-12 h-12 animate-pulse" />
            </div>

            <div class="p-6">
                <div class="flex justify-between items-start">
                    <div>
                        <h1 class="text-2xl font-bold text-gray-900">iPhone 17 Pro</h1>
                        <p class="text-sm text-gray-500 uppercase tracking-widest mt-1">Flash Sale Edition</p>
                    </div>
                    <span class="bg-red-100 text-red-600 text-xs font-bold px-2 py-1 rounded">LIVE</span>
                </div>

                <div class="mt-6 flex items-baseline gap-2">
                    <span class="text-4xl font-extrabold text-gray-900">₹1,29,900</span>
                    <span class="text-gray-400 line-through">₹1,39,900</span>
                </div>

                <div class="mt-4">
                    <div class="flex justify-between text-sm mb-1">
                        <span class="font-medium text-gray-700">Stock Remaining</span>
                        <span :class="stock < 10 ? 'text-red-500 font-bold' : 'text-gray-600'">{{ stock }} / 100</span>
                    </div>
                    <div class="w-full bg-gray-200 rounded-full h-2">
                        <div class="bg-black h-2 rounded-full transition-all duration-500" :style="{ width: stock + '%' }"></div>
                    </div>
                </div>

                <button 
                    @click="handlePurchase"
                    :disabled="loading || stock <= 0"
                    class="mt-8 w-full bg-black text-white py-4 rounded-xl font-bold flex items-center justify-center gap-2 hover:bg-gray-800 transition-all active:scale-95 disabled:bg-gray-400 disabled:scale-100"
                >
                    <template v-if="loading">
                        <div class="animate-spin rounded-full h-5 w-5 border-2 border-white border-t-transparent"></div>
                        Processing...
                    </template>
                    <template v-else>
                        <ShoppingCart class="w-5 h-5" />
                        BUY NOW
                    </template>
                </button>

                <div v-if="status" :class="status === 'success' ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'" class="mt-4 p-3 rounded-lg flex items-center gap-2 text-sm font-medium">
                    <CheckCircle v-if="status === 'success'" class="w-4 h-4" />
                    <AlertCircle v-else class="w-4 h-4" />
                    {{ message }}
                </div>
            </div>
        </div>
    </div>
    <div class="mt-10 max-w-md mx-auto">
        <h3 class="text-lg font-bold text-gray-800 mb-4 flex items-center gap-2">
            <Zap class="w-5 h-5 text-yellow-500" /> Recent Activity
        </h3>
        <div class="bg-white rounded-xl shadow-inner p-4 h-40 overflow-y-auto border border-gray-200">
            <div v-if="orders.length === 0" class="text-gray-400 text-sm text-center mt-10">
                No orders processed yet...
            </div>
            <div v-for="(ord, index) in orders" :key="index" class="text-xs py-2 border-b border-gray-50 flex justify-between">
                <span class="font-mono text-blue-600">{{ ord.userId }}</span>
                <span class="text-gray-500">{{ ord.time }}</span>
                <span class="text-green-600 font-bold">PROCESSED</span>
            </div>
        </div>
    </div>
</template>