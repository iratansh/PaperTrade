import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import type {
  Portfolio,
  Order,
  PlaceOrderRequest,
  StockQuote,
  SymbolMatch,
  Candle,
  HistoryRange,
  AuthResponse,
} from '../types';

// --- Token storage -------------------------------------------------------

const ACCESS_KEY = 'papertrade_access_token';
const REFRESH_KEY = 'papertrade_refresh_token';
const USER_KEY = 'papertrade_user';

export const tokenStore = {
  getAccess: () => localStorage.getItem(ACCESS_KEY),
  getRefresh: () => localStorage.getItem(REFRESH_KEY),
  getUser: () => {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  },
  save: (auth: AuthResponse) => {
    localStorage.setItem(ACCESS_KEY, auth.accessToken);
    localStorage.setItem(REFRESH_KEY, auth.refreshToken);
    localStorage.setItem(
      USER_KEY,
      JSON.stringify({ userId: auth.userId, username: auth.username })
    );
  },
  clear: () => {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
  },
};

// --- Axios instance ------------------------------------------------------

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

// Attach the access token to every request
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStore.getAccess();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// On 401, try a one-time refresh, then retry the original request
let refreshing: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = tokenStore.getRefresh();
  if (!refreshToken) return null;
  try {
    const { data } = await axios.post<AuthResponse>('/api/auth/refresh', { refreshToken });
    tokenStore.save(data);
    return data.accessToken;
  } catch {
    tokenStore.clear();
    return null;
  }
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as InternalAxiosRequestConfig & { _retried?: boolean };
    const isAuthCall = original?.url?.includes('/auth/');

    if (error.response?.status === 401 && original && !original._retried && !isAuthCall) {
      original._retried = true;
      refreshing = refreshing ?? refreshAccessToken();
      const newToken = await refreshing;
      refreshing = null;

      if (newToken) {
        original.headers.Authorization = `Bearer ${newToken}`;
        return api(original);
      }
      // Refresh failed: force re-login
      tokenStore.clear();
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// --- API modules ---------------------------------------------------------

export const authApi = {
  register: async (username: string, email: string, password: string): Promise<AuthResponse> => {
    const { data } = await api.post('/auth/register', { username, email, password });
    return data;
  },
  login: async (username: string, password: string): Promise<AuthResponse> => {
    const { data } = await api.post('/auth/login', { username, password });
    return data;
  },
};

export const portfolioApi = {
  getPortfolio: async (): Promise<Portfolio> => {
    const { data } = await api.get('/portfolio');
    return data;
  },
};

export const ordersApi = {
  placeOrder: async (request: PlaceOrderRequest): Promise<Order> => {
    const { data } = await api.post('/orders', request);
    return data;
  },
  getOrderHistory: async (): Promise<Order[]> => {
    const { data } = await api.get('/orders');
    return data;
  },
  cancelOrder: async (orderId: string): Promise<Order> => {
    const { data } = await api.delete(`/orders/${orderId}`);
    return data;
  },
};

export const marketDataApi = {
  getQuote: async (symbol: string): Promise<StockQuote> => {
    const { data } = await api.get(`/stocks/${symbol}/quote`);
    return data;
  },
  getHistory: async (symbol: string, range: HistoryRange): Promise<Candle[]> => {
    const { data } = await api.get(`/stocks/${symbol}/history?range=${range}`);
    return data;
  },
  searchStocks: async (query: string): Promise<SymbolMatch[]> => {
    const { data } = await api.get(`/stocks/search?q=${encodeURIComponent(query)}`);
    return data;
  },
};

export default api;
