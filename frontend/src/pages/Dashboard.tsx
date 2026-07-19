import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Wallet, TrendingUp } from 'lucide-react';
import PortfolioChart from '../components/PortfolioChart';
import PositionCard from '../components/PositionCard';
import { portfolioApi } from '../services/api';
import { formatCurrency, formatPercent } from '../utils/format';
import type { Portfolio } from '../types';

// Temporary hardcoded user/account IDs (until we add auth)
const TEMP_USER_ID = '123e4567-e89b-12d3-a456-426614174000';

export default function Dashboard() {
  const navigate = useNavigate();
  const [portfolio, setPortfolio] = useState<Portfolio | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadPortfolio();
  }, []);

  const loadPortfolio = async () => {
    try {
      setLoading(true);
      const data = await portfolioApi.getPortfolio(TEMP_USER_ID);
      setPortfolio(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load portfolio');
      console.error('Error loading portfolio:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading portfolio...</p>
        </div>
      </div>
    );
  }

  if (error || !portfolio) {
    return (
      <div className="flex items-center justify-center min-h-screen">
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

  // Mock chart data (in production, fetch historical data)
  const mockChartLabels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'];
  const mockChartData = [
    portfolio.totalPortfolioValue - 2000,
    portfolio.totalPortfolioValue - 1500,
    portfolio.totalPortfolioValue - 800,
    portfolio.totalPortfolioValue + 200,
    portfolio.totalPortfolioValue,
  ];

  const isPositive = portfolio.totalGainLoss >= 0;

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Portfolio Summary */}
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 mb-6">
          <div className="flex items-center justify-between mb-6">
            <div>
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
                <p className="text-xs text-gray-500">Cash Balance</p>
                <p className="text-lg font-semibold text-gray-900">
                  {formatCurrency(portfolio.cashBalance)}
                </p>
              </div>
            </div>
          </div>

          <PortfolioChart data={mockChartData} labels={mockChartLabels} />
        </div>

        {/* Positions */}
        <div className="mb-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-semibold text-gray-900">Your Positions</h2>
            <button
              onClick={() => navigate('/search')}
              className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors"
            >
              Add Position
            </button>
          </div>

          {portfolio.positions.length === 0 ? (
            <div className="bg-white rounded-lg border border-gray-200 p-12 text-center">
              <TrendingUp size={48} className="text-gray-300 mx-auto mb-4" />
              <h3 className="text-lg font-semibold text-gray-900 mb-2">
                No positions yet
              </h3>
              <p className="text-gray-600 mb-6">
                Start building your portfolio by buying your first stock
              </p>
              <button
                onClick={() => navigate('/search')}
                className="px-6 py-3 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors"
              >
                Search Stocks
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {portfolio.positions.map((position) => (
                <PositionCard
                  key={position.symbol}
                  position={position}
                  onClick={(symbol) => navigate(`/stock/${symbol}`)}
                />
              ))}
            </div>
          )}
        </div>

        {/* Stats */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-white rounded-lg border border-gray-200 p-4">
            <p className="text-sm text-gray-500 mb-1">Total Invested</p>
            <p className="text-2xl font-bold text-gray-900">
              {formatCurrency(100000 - portfolio.cashBalance)}
            </p>
          </div>
          <div className="bg-white rounded-lg border border-gray-200 p-4">
            <p className="text-sm text-gray-500 mb-1">Position Value</p>
            <p className="text-2xl font-bold text-gray-900">
              {formatCurrency(portfolio.totalPositionValue)}
            </p>
          </div>
          <div className="bg-white rounded-lg border border-gray-200 p-4">
            <p className="text-sm text-gray-500 mb-1">Number of Positions</p>
            <p className="text-2xl font-bold text-gray-900">
              {portfolio.positions.length}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
