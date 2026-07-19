import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Wallet, TrendingUp, TrendingDown, ChevronRight } from 'lucide-react';
import PortfolioChart from '../components/PortfolioChart';
import { portfolioApi } from '../services/api';
import { formatCurrency, formatPercent } from '../utils/format';
import type { Portfolio, Position, SnapshotPoint } from '../types';

function formatSnapshotLabel(iso: string): string {
  const d = new Date(iso);
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  }).format(d);
}

export default function Dashboard() {
  const navigate = useNavigate();
  const [portfolio, setPortfolio] = useState<Portfolio | null>(null);
  const [history, setHistory] = useState<SnapshotPoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadPortfolio();
  }, []);

  const loadPortfolio = async () => {
    try {
      setLoading(true);
      setError(null); // clear any stale error before (re)loading
      const [data, snapshots] = await Promise.all([
        portfolioApi.getPortfolio(),
        portfolioApi.getHistory().catch(() => [] as SnapshotPoint[]),
      ]);
      setPortfolio(data);
      setHistory(snapshots);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load portfolio');
      console.error('Error loading portfolio:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[70vh]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto" />
          <p className="mt-4 text-gray-600">Loading portfolio…</p>
        </div>
      </div>
    );
  }

  if (error || !portfolio) {
    return (
      <div className="flex items-center justify-center min-h-[70vh]">
        <div className="text-center">
          <p className="text-red-600 mb-4">{error || 'No portfolio data'}</p>
          <button
            onClick={loadPortfolio}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  // Real portfolio growth from recorded snapshots
  const hasHistory = history.length >= 2;
  const chartLabels = history.map((s) => formatSnapshotLabel(s.capturedAt));
  const chartData = history.map((s) => s.totalValue);

  const isPositive = portfolio.totalGainLoss >= 0;

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Portfolio summary */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-6">
          <div className="flex items-start justify-between mb-6">
            <div>
              <p className="text-sm text-gray-500 mb-1">Portfolio value</p>
              <h1 className="text-3xl font-bold text-gray-900">
                {formatCurrency(portfolio.totalPortfolioValue)}
              </h1>
              <div className="flex items-center gap-2 mt-2">
                <span className={`text-lg font-semibold ${isPositive ? 'text-success' : 'text-danger'}`}>
                  {isPositive ? '+' : ''}{formatCurrency(portfolio.totalGainLoss)}
                </span>
                <span className={`text-sm font-medium ${isPositive ? 'text-success' : 'text-danger'}`}>
                  ({formatPercent(portfolio.totalGainLossPercentage)})
                </span>
              </div>
            </div>
            <div className="flex items-center gap-2 px-4 py-2 bg-gray-50 rounded-lg">
              <Wallet size={20} className="text-gray-600" />
              <div>
                <p className="text-xs text-gray-500">Cash</p>
                <p className="text-lg font-semibold text-gray-900">
                  {formatCurrency(portfolio.cashBalance)}
                </p>
              </div>
            </div>
          </div>

          {hasHistory ? (
            <PortfolioChart data={chartData} labels={chartLabels} />
          ) : (
            <div className="h-64 flex flex-col items-center justify-center text-center text-gray-400">
              <p className="font-medium text-gray-500">Building your growth chart…</p>
              <p className="text-sm mt-1">
                Portfolio value is recorded periodically — check back shortly.
              </p>
            </div>
          )}
        </div>

        {/* Holdings — vertical column, Wealthsimple style */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="px-6 py-4 border-b border-gray-100">
            <h2 className="text-lg font-semibold text-gray-900">Holdings</h2>
          </div>

          {portfolio.positions.length === 0 ? (
            <div className="p-12 text-center">
              <TrendingUp size={48} className="text-gray-300 mx-auto mb-4" />
              <h3 className="text-lg font-semibold text-gray-900 mb-2">No holdings yet</h3>
              <p className="text-gray-600 mb-6">Buy your first stock to start building your portfolio.</p>
              <button
                onClick={() => navigate('/search')}
                className="px-6 py-3 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors"
              >
                Search Stocks
              </button>
            </div>
          ) : (
            <ul className="divide-y divide-gray-100">
              {portfolio.positions.map((position) => (
                <PositionRow
                  key={position.symbol}
                  position={position}
                  onClick={() => navigate(`/stock/${position.symbol}`)}
                />
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}

function PositionRow({ position, onClick }: { position: Position; onClick: () => void }) {
  const isPositive = position.unrealizedPnL >= 0;

  return (
    <li>
      <button
        onClick={onClick}
        className="w-full flex items-center justify-between px-6 py-4 hover:bg-gray-50 transition-colors text-left"
      >
        <div className="flex items-center gap-4">
          <div className="w-10 h-10 rounded-full bg-primary-100 flex items-center justify-center shrink-0">
            <span className="text-sm font-bold text-primary-700">
              {position.symbol.slice(0, 2)}
            </span>
          </div>
          <div>
            <p className="font-semibold text-gray-900">{position.symbol}</p>
            <p className="text-sm text-gray-500">
              {position.quantity} shares · {formatCurrency(position.averageCost)} avg
            </p>
          </div>
        </div>

        <div className="flex items-center gap-4">
          <div className="text-right">
            <p className="font-semibold text-gray-900">{formatCurrency(position.currentValue)}</p>
            <p className={`text-sm font-medium flex items-center gap-1 justify-end ${isPositive ? 'text-success' : 'text-danger'}`}>
              {isPositive ? <TrendingUp size={14} /> : <TrendingDown size={14} />}
              {isPositive ? '+' : ''}{formatCurrency(position.unrealizedPnL)} ({formatPercent(position.unrealizedPnLPercentage)})
            </p>
          </div>
          <ChevronRight size={20} className="text-gray-300" />
        </div>
      </button>
    </li>
  );
}
