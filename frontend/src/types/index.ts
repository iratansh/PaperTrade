export interface User {
  userId: string;
  username: string;
  email: string;
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

export interface PlaceOrderRequest {
  accountId: string;
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
