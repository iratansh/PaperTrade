import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search as SearchIcon, TrendingUp, ArrowLeft } from 'lucide-react';

// Popular stocks for quick access
const POPULAR_STOCKS = [
  { symbol: 'AAPL', name: 'Apple Inc.' },
  { symbol: 'GOOGL', name: 'Alphabet Inc.' },
  { symbol: 'MSFT', name: 'Microsoft Corporation' },
  { symbol: 'AMZN', name: 'Amazon.com Inc.' },
  { symbol: 'TSLA', name: 'Tesla Inc.' },
  { symbol: 'META', name: 'Meta Platforms Inc.' },
  { symbol: 'NVDA', name: 'NVIDIA Corporation' },
  { symbol: 'JPM', name: 'JPMorgan Chase & Co.' },
];

export default function Search() {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');

  const filteredStocks = POPULAR_STOCKS.filter(
    (stock) =>
      stock.symbol.toLowerCase().includes(searchQuery.toLowerCase()) ||
      stock.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleStockClick = (symbol: string) => {
    navigate(`/stock/${symbol}`);
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/stock/${searchQuery.toUpperCase()}`);
    }
  };

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
          <h1 className="text-2xl font-bold text-gray-900 mb-6">Search Stocks</h1>

          <form onSubmit={handleSearch} className="mb-8">
            <div className="relative">
              <SearchIcon
                className="absolute left-4 top-1/2 transform -translate-y-1/2 text-gray-400"
                size={20}
              />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search by symbol or company name..."
                className="w-full pl-12 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-600 focus:border-transparent text-lg"
              />
            </div>
          </form>

          <div>
            <h2 className="text-lg font-semibold text-gray-900 mb-4">
              {searchQuery ? 'Search Results' : 'Popular Stocks'}
            </h2>
            <div className="space-y-2">
              {filteredStocks.length === 0 ? (
                <div className="text-center py-8">
                  <p className="text-gray-500 mb-4">
                    No results found. Try entering a stock symbol.
                  </p>
                  <button
                    onClick={() => handleStockClick(searchQuery.toUpperCase())}
                    className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700"
                    disabled={!searchQuery.trim()}
                  >
                    Search for "{searchQuery.toUpperCase()}"
                  </button>
                </div>
              ) : (
                filteredStocks.map((stock) => (
                  <button
                    key={stock.symbol}
                    onClick={() => handleStockClick(stock.symbol)}
                    className="w-full flex items-center justify-between p-4 rounded-lg border border-gray-200 hover:bg-gray-50 hover:border-primary-300 transition-all"
                  >
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 bg-primary-100 rounded-full flex items-center justify-center">
                        <TrendingUp size={20} className="text-primary-600" />
                      </div>
                      <div className="text-left">
                        <p className="font-semibold text-gray-900">{stock.symbol}</p>
                        <p className="text-sm text-gray-500">{stock.name}</p>
                      </div>
                    </div>
                    <svg
                      className="w-5 h-5 text-gray-400"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M9 5l7 7-7 7"
                      />
                    </svg>
                  </button>
                ))
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
