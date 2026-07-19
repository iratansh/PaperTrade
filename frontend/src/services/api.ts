import axios from 'axios';
import type { Portfolio, Order, PlaceOrderRequest, StockQuote } from '../types';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

export const portfolioApi = {
  getPortfolio: async (userId: string): Promise<Portfolio> => {
    const { data } = await api.get(`/portfolio?userId=${userId}`);
    return data;
  },
};

export const ordersApi = {
  placeOrder: async (request: PlaceOrderRequest): Promise<Order> => {
    const { data } = await api.post('/orders', request);
    return data;
  },

  getOrderHistory: async (accountId: string): Promise<Order[]> => {
    const { data } = await api.get(`/orders?accountId=${accountId}`);
    return data;
  },

  cancelOrder: async (orderId: string, accountId: string): Promise<Order> => {
    const { data } = await api.delete(`/orders/${orderId}?accountId=${accountId}`);
    return data;
  },
};

export const marketDataApi = {
  getQuote: async (symbol: string): Promise<StockQuote> => {
    const { data } = await api.get(`/stocks/${symbol}/quote`);
    return data;
  },

  searchStocks: async (query: string): Promise<string> => {
    const { data } = await api.get(`/stocks/search?q=${query}`);
    return data;
  },
};

export default api;
