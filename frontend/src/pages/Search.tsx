import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search as SearchIcon, TrendingUp, ArrowLeft, Loader2 } from 'lucide-react';
import { marketDataApi } from '../services/api';
import type { SymbolMatch } from '../types';

export default function Search() {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SymbolMatch[]>([]);
  const [loading, setLoading] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);

    const trimmed = query.trim();
    if (trimmed.length < 1) {
      setResults([]);
      setLoading(false);
      return;
    }

    setLoading(true);
    debounceRef.current = setTimeout(async () => {
      try {
        const matches = await marketDataApi.searchStocks(trimmed);
        setResults(matches);
      } catch (err) {
        console.error('Search failed:', err);
        setResults([]);
      } finally {
        setLoading(false);
      }
    }, 300);

    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [query]);

  const handleStockClick = (symbol: string) => {
    navigate(`/stock/${symbol}`);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <button
          onClick={() => navigate('/dashboard')}
          className="flex items-center gap-2 text-gray-600 hover:text-gray-900 mb-6"
        >
          <ArrowLeft size={20} />
          <span>Back to Dashboard</span>
        </button>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h1 className="text-2xl font-bold text-gray-900 mb-6">Search Stocks</h1>

          <div className="relative mb-6">
            <SearchIcon
              className="absolute left-4 top-1/2 transform -translate-y-1/2 text-gray-400"
              size={20}
            />
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search by symbol or company name…"
              autoFocus
              className="w-full pl-12 pr-12 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-600 focus:border-transparent text-lg"
            />
            {loading && (
              <Loader2
                className="absolute right-4 top-1/2 transform -translate-y-1/2 text-gray-400 animate-spin"
                size={20}
              />
            )}
          </div>

          <div className="space-y-2">
            {!loading && query.trim() && results.length === 0 && (
              <p className="text-center text-gray-500 py-8">
                No matches for "{query}". Try a different ticker or company name.
              </p>
            )}

            {results.map((stock) => (
              <button
                key={stock.symbol}
                onClick={() => handleStockClick(stock.symbol)}
                className="w-full flex items-center justify-between p-4 rounded-lg border border-gray-200 hover:bg-gray-50 hover:border-primary-300 transition-all"
              >
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-primary-100 rounded-full flex items-center justify-center shrink-0">
                    <TrendingUp size={20} className="text-primary-600" />
                  </div>
                  <div className="text-left">
                    <p className="font-semibold text-gray-900">{stock.symbol}</p>
                    <p className="text-sm text-gray-500 line-clamp-1">{stock.description}</p>
                  </div>
                </div>
                <span className="text-gray-400">›</span>
              </button>
            ))}

            {!query.trim() && (
              <p className="text-center text-gray-400 py-8">
                Start typing to search thousands of stocks.
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
