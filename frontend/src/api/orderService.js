import axios from 'axios';
import axiosRetry from 'axios-retry';

const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json'
  }
});

// Configure axios to retry automatically on network errors or 5xx errors
axiosRetry(apiClient, { 
  retries: 3, // Retry 3 times
  retryDelay: axiosRetry.exponentialDelay, // Wait longer between each try (1s, 2s, 4s...)
  retryCondition: (error) => {
    // Only retry if it's a network error or a server error (500-599)
    // Do NOT retry on 409 (Idempotency hit) or 400 (Bad request)
    return axiosRetry.isNetworkOrIdempotentRequestError(error) || error.response?.status >= 500;
  }
});

export const orderService = {
  async placeOrder(productId, userId, idempotencyKey) {
    const response = await apiClient.post('/orders/buy', {
      productId: productId,
      userId: userId
    }, {
      headers: {
        'X-Idempotency-Key': idempotencyKey
      }
    });
    return response.data;
  }
};