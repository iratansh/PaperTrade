import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, TrendingUp } from 'lucide-react';
import StockChart from '../components/StockChart';
import { marketDataApi, ordersApi, portfolioApi } from '../services/api';
import { formatCurrency } from '../utils/format';
import { useToast } from '../context/ToastContext';
import type { StockQuote, PlaceOrderRequest, Candle, HistoryRange, Position } from '../types';

const RANGES: HistoryRange[] = ['1D', '1W', '3M', '1Y', 'YTD'];

export default function StockDetail() {
  const { symbol } = useParams<{ symbol: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [quote, setQuote] = useState<StockQuote | null>(null);
  const [loading, setLoading] = useState(true);

  const [candles, setCandles] = useState<Candle[]>([]);
  const [range, setRange] = useState<HistoryRange>('3M');
  const [chartLoading, setChartLoading] = useState(false);

  const [orderType, setOrderType] = useState<'BUY' | 'SELL'>('BUY');
  const [quantity, setQuantity] = useState('1');
  const [limitPrice, setLimitPrice] = useState('');
  const [isMarketOrder, setIsMarketOrder] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const [cashBalance, setCashBalance] = useState(0);
  const [heldShares, setHeldShares] = useState(0);

  useEffect(() => {
    if (symbol) {
      loadStockData();
      loadAccountState();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [symbol]);

  useEffect(() => {
    if (symbol) loadHistory(range);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [symbol, range]);

  const loadStockData = async () => {
    if (!symbol) return;
    try {
      setLoading(true);
      const data = await marketDataApi.getQuote(symbol);
      setQuote(data);
      setLimitPrice(data.price.toString());
    } catch (err) {
      console.error('Error loading stock:', err);
      showToast('Failed to load stock data', 'error');
    } finally {
      setLoading(false);
    }
  };

  const loadHistory = async (r: HistoryRange) => {
    if (!symbol) return;
    try {
      setChartLoading(true);
      const data = await marketDataApi.getHistory(symbol, r);
      setCandles(data);
    } catch (err) {
      console.error('Error loading history:', err);
      setCandles([]);
    } finally {
      setChartLoading(false);
    }
  };

  const loadAccountState = async () => {
    try {
      const portfolio = await portfolioApi.getPortfolio();
      setCashBalance(portfolio.cashBalance);
      const pos = portfolio.positions.find(
        (p: Position) => p.symbol === symbol?.toUpperCase()
      );
      setHeldShares(pos?.quantity ?? 0);
    } catch (err) {
      console.error('Error loading account state:', err);
    }
  };

  const handlePlaceOrder = async () => {
    if (!symbol || !quote) return;

    const qty = parseFloat(quantity);
    if (isNaN(qty) || qty <= 0) {
      showToast('Please enter a valid quantity', 'error');
      return;
    }

    const price = isMarketOrder ? quote.price : parseFloat(limitPrice);
    const estimatedCost = qty * price;

    if (orderType === 'BUY' && estimatedCost > cashBalance) {
      showToast(
        `Insufficient funds. Need ${formatCurrency(estimatedCost)}, have ${formatCurrency(cashBalance)}`,
        'error'
      );
      return;
    }
    if (orderType === 'SELL' && qty > heldShares) {
      showToast(`You only own ${heldShares} shares of ${symbol.toUpperCase()}`, 'error');
      return;
    }

    try {
      setSubmitting(true);
      const request: PlaceOrderRequest = {
        symbol: symbol.toUpperCase(),
        type: isMarketOrder ? 'MARKET' : 'LIMIT',
        side: orderType,
        quantity: qty,
        limitPrice: isMarketOrder ? undefined : parseFloat(limitPrice),
        idempotencyKey: `order-${Date.now()}-${Math.random()}`,
      };
      const order = await ordersApi.placeOrder(request);
      showToast(
        `${order.side} ${order.filledQuantity} ${order.symbol} @ ${formatCurrency(order.filledPrice || 0)}`,
        'success'
      );
      navigate('/dashboard');
    } catch (err: any) {
      showToast(err.response?.data?.message || 'Failed to place order', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600" />
      </div>
    );
  }

  if (!quote) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <p className="text-red-600">Failed to load stock data</p>
      </div>
    );
  }

  const estimatedTotal =
    parseFloat(quantity || '0') * (isMarketOrder ? quote.price : parseFloat(limitPrice || '0'));

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <button
          onClick={() => navigate('/dashboard')}
          className="flex items-center gap-2 text-gray-600 hover:text-gray-900 mb-6"
        >
          <ArrowLeft size={20} />
          <span>Back to Dashboard</span>
        </button>

        {/* Price + chart */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-6">
          <div className="flex items-center gap-3 mb-4">
            <TrendingUp size={28} className="text-primary-600" />
            <div>
              <h1 className="text-2xl font-bold text-gray-900">{symbol?.toUpperCase()}</h1>
              <p className="text-3xl font-bold text-gray-900">{formatCurrency(quote.price)}</p>
            </div>
          </div>

          <div className={chartLoading ? 'opacity-50 transition-opacity' : ''}>
            <StockChart candles={candles} />
          </div>

          <div className="flex gap-2 mt-4">
            {RANGES.map((r) => (
              <button
                key={r}
                onClick={() => setRange(r)}
                className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                  range === r
                    ? 'bg-primary-600 text-white'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                {r}
              </button>
            ))}
          </div>
        </div>

        {/* Order form */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-semibold text-gray-900">Place Order</h2>
            <div className="text-right text-sm">
              <p className="text-gray-500">Cash: {formatCurrency(cashBalance)}</p>
              {heldShares > 0 && (
                <p className="text-gray-500">Holding: {heldShares} shares</p>
              )}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-2 mb-6">
            <button
              onClick={() => setOrderType('BUY')}
              className={`py-3 rounded-lg font-semibold transition-colors ${
                orderType === 'BUY' ? 'bg-success text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              Buy
            </button>
            <button
              onClick={() => setOrderType('SELL')}
              className={`py-3 rounded-lg font-semibold transition-colors ${
                orderType === 'SELL' ? 'bg-danger text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              Sell
            </button>
          </div>

          <div className="mb-6">
            <label className="block text-sm font-medium text-gray-700 mb-2">Order Type</label>
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

          <div className="mb-6">
            <label className="block text-sm font-medium text-gray-700 mb-2">Quantity (Shares)</label>
            <input
              type="number"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              min="0.0001"
              step="1"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-600 focus:border-transparent"
            />
          </div>

          {!isMarketOrder && (
            <div className="mb-6">
              <label className="block text-sm font-medium text-gray-700 mb-2">Limit Price</label>
              <input
                type="number"
                value={limitPrice}
                onChange={(e) => setLimitPrice(e.target.value)}
                min="0.01"
                step="0.01"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-600 focus:border-transparent"
              />
            </div>
          )}

          <div className="bg-gray-50 rounded-lg p-4 mb-6 flex justify-between items-center">
            <span className="text-gray-600">Estimated Total</span>
            <span className="text-2xl font-bold text-gray-900">{formatCurrency(estimatedTotal)}</span>
          </div>

          <button
            onClick={handlePlaceOrder}
            disabled={submitting}
            className={`w-full py-3 rounded-lg font-semibold text-white transition-colors ${
              orderType === 'BUY' ? 'bg-success hover:bg-green-600' : 'bg-danger hover:bg-red-600'
            } ${submitting ? 'opacity-50 cursor-not-allowed' : ''}`}
          >
            {submitting ? 'Placing Order…' : `${orderType} ${symbol?.toUpperCase()}`}
          </button>
        </div>
      </div>
    </div>
  );
}
