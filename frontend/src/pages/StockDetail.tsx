import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, TrendingUp } from 'lucide-react';
import { marketDataApi, ordersApi, portfolioApi } from '../services/api';
import { formatCurrency } from '../utils/format';
import type { StockQuote, PlaceOrderRequest } from '../types';

// Temporary hardcoded IDs
const TEMP_USER_ID = '123e4567-e89b-12d3-a456-426614174000';
const TEMP_ACCOUNT_ID = '223e4567-e89b-12d3-a456-426614174000';

export default function StockDetail() {
  const { symbol } = useParams<{ symbol: string }>();
  const navigate = useNavigate();

  const [quote, setQuote] = useState<StockQuote | null>(null);
  const [loading, setLoading] = useState(true);
  const [orderType, setOrderType] = useState<'BUY' | 'SELL'>('BUY');
  const [quantity, setQuantity] = useState<string>('1');
  const [limitPrice, setLimitPrice] = useState<string>('');
  const [isMarketOrder, setIsMarketOrder] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [cashBalance, setCashBalance] = useState<number>(0);

  useEffect(() => {
    if (symbol) {
      loadStockData();
      loadCashBalance();
    }
  }, [symbol]);

  const loadStockData = async () => {
    if (!symbol) return;

    try {
      setLoading(true);
      const data = await marketDataApi.getQuote(symbol);
      setQuote(data);
      setLimitPrice(data.price.toString());
    } catch (err) {
      console.error('Error loading stock:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadCashBalance = async () => {
    try {
      const portfolio = await portfolioApi.getPortfolio(TEMP_USER_ID);
      setCashBalance(portfolio.cashBalance);
    } catch (err) {
      console.error('Error loading cash balance:', err);
    }
  };

  const handlePlaceOrder = async () => {
    if (!symbol || !quote) return;

    const qty = parseFloat(quantity);
    if (isNaN(qty) || qty <= 0) {
      alert('Please enter a valid quantity');
      return;
    }

    const estimatedCost = qty * quote.price;
    if (orderType === 'BUY' && estimatedCost > cashBalance) {
      alert(`Insufficient funds. You need ${formatCurrency(estimatedCost)} but have ${formatCurrency(cashBalance)}`);
      return;
    }

    try {
      setSubmitting(true);

      const request: PlaceOrderRequest = {
        accountId: TEMP_ACCOUNT_ID,
        symbol: symbol.toUpperCase(),
        type: isMarketOrder ? 'MARKET' : 'LIMIT',
        side: orderType,
        quantity: qty,
        limitPrice: isMarketOrder ? undefined : parseFloat(limitPrice),
        idempotencyKey: `order-${Date.now()}-${Math.random()}`,
      };

      const order = await ordersApi.placeOrder(request);

      alert(`Order placed successfully!\n${order.side} ${order.filledQuantity} shares of ${order.symbol} at ${formatCurrency(order.filledPrice || 0)}`);

      navigate('/dashboard');
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || 'Failed to place order';
      alert(`Error: ${errorMsg}`);
      console.error('Error placing order:', err);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (!quote) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <p className="text-red-600">Failed to load stock data</p>
      </div>
    );
  }

  const estimatedTotal = parseFloat(quantity || '0') * (isMarketOrder ? quote.price : parseFloat(limitPrice || '0'));

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <button
          onClick={() => navigate('/dashboard')}
          className="flex items-center gap-2 text-gray-600 hover:text-gray-900 mb-6"
        >
          <ArrowLeft size={20} />
          <span>Back to Dashboard</span>
        </button>

        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 mb-6">
          <div className="flex items-center gap-3 mb-4">
            <TrendingUp size={32} className="text-primary-600" />
            <div>
              <h1 className="text-3xl font-bold text-gray-900">{symbol}</h1>
              <p className="text-sm text-gray-500">Stock</p>
            </div>
          </div>

          <div className="mb-6">
            <p className="text-sm text-gray-500 mb-1">Current Price</p>
            <p className="text-4xl font-bold text-gray-900">{formatCurrency(quote.price)}</p>
          </div>

          <div className="bg-gray-50 rounded-lg p-4">
            <p className="text-sm text-gray-600 mb-1">Available Cash</p>
            <p className="text-xl font-semibold text-gray-900">{formatCurrency(cashBalance)}</p>
          </div>
        </div>

        {/* Order Form */}
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-6">Place Order</h2>

          {/* Buy/Sell Toggle */}
          <div className="grid grid-cols-2 gap-2 mb-6">
            <button
              onClick={() => setOrderType('BUY')}
              className={`py-3 rounded-lg font-semibold transition-colors ${
                orderType === 'BUY'
                  ? 'bg-success text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              Buy
            </button>
            <button
              onClick={() => setOrderType('SELL')}
              className={`py-3 rounded-lg font-semibold transition-colors ${
                orderType === 'SELL'
                  ? 'bg-danger text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              Sell
            </button>
          </div>

          {/* Order Type */}
          <div className="mb-6">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Order Type
            </label>
            <div className="grid grid-cols-2 gap-2">
              <button
                onClick={() => setIsMarketOrder(true)}
                className={`py-2 px-4 rounded-lg border ${
                  isMarketOrder
                    ? 'border-primary-600 bg-primary-50 text-primary-700'
                    : 'border-gray-300 text-gray-700 hover:bg-gray-50'
                }`}
              >
                Market
              </button>
              <button
                onClick={() => setIsMarketOrder(false)}
                className={`py-2 px-4 rounded-lg border ${
                  !isMarketOrder
                    ? 'border-primary-600 bg-primary-50 text-primary-700'
                    : 'border-gray-300 text-gray-700 hover:bg-gray-50'
                }`}
              >
                Limit
              </button>
            </div>
          </div>

          {/* Quantity */}
          <div className="mb-6">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Quantity (Shares)
            </label>
            <input
              type="number"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              min="0.0001"
              step="1"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-600 focus:border-transparent"
              placeholder="Enter quantity"
            />
          </div>

          {/* Limit Price (only for limit orders) */}
          {!isMarketOrder && (
            <div className="mb-6">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Limit Price
              </label>
              <input
                type="number"
                value={limitPrice}
                onChange={(e) => setLimitPrice(e.target.value)}
                min="0.01"
                step="0.01"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-600 focus:border-transparent"
                placeholder="Enter limit price"
              />
            </div>
          )}

          {/* Estimated Total */}
          <div className="bg-gray-50 rounded-lg p-4 mb-6">
            <div className="flex justify-between items-center">
              <span className="text-gray-600">Estimated Total</span>
              <span className="text-2xl font-bold text-gray-900">
                {formatCurrency(estimatedTotal)}
              </span>
            </div>
          </div>

          {/* Submit Button */}
          <button
            onClick={handlePlaceOrder}
            disabled={submitting}
            className={`w-full py-3 rounded-lg font-semibold text-white transition-colors ${
              orderType === 'BUY'
                ? 'bg-success hover:bg-green-600'
                : 'bg-danger hover:bg-red-600'
            } ${submitting ? 'opacity-50 cursor-not-allowed' : ''}`}
          >
            {submitting ? 'Placing Order...' : `${orderType} ${symbol}`}
          </button>
        </div>
      </div>
    </div>
  );
}
