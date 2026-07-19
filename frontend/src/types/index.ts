export interface User {
  userId: string;
  username: string;
  email: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  username: string;
}

export interface Position {
  symbol: string;
  quantity: number;
  averageCost: number;
  currentPrice: number;
  currentValue: number;
  unrealizedPnL: number;
  unrealizedPnLPercentage: number;
}

export interface Portfolio {
  cashBalance: number;
  totalPositionValue: number;
  totalPortfolioValue: number;
  totalGainLoss: number;
  totalGainLossPercentage: number;
  positions: Position[];
}

export interface Order {
  orderId: string;
  symbol: string;
  type: 'MARKET' | 'LIMIT';
  side: 'BUY' | 'SELL';
  quantity: number;
  limitPrice?: number;
  status: 'PENDING' | 'FILLED' | 'CANCELLED' | 'REJECTED';
  filledPrice?: number;
  filledQuantity?: number;
  createdAt: string;
  filledAt?: string;
}

// accountId is resolved server-side from the JWT, so it's not sent by the client
export interface PlaceOrderRequest {
  symbol: string;
  type: 'MARKET' | 'LIMIT';
  side: 'BUY' | 'SELL';
  quantity: number;
  limitPrice?: number;
  idempotencyKey?: string;
}

export interface StockQuote {
  symbol: string;
  price: number;
}

export interface SymbolMatch {
  symbol: string;
  description: string;
}

export interface Candle {
  datetime: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export type HistoryRange = '1D' | '1W' | '3M' | '1Y' | 'YTD';

export interface SnapshotPoint {
  capturedAt: string;
  totalValue: number;
}
