import { Search, TrendingUp, LogOut } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="bg-white border-b border-gray-200 sticky top-0 z-50">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          <Link to="/dashboard" className="flex items-center gap-2">
            <TrendingUp className="text-primary-600" size={28} />
            <span className="text-xl font-bold text-gray-900">PaperTrade</span>
          </Link>

          <div className="flex items-center gap-2">
            <button
              onClick={() => navigate('/search')}
              className="flex items-center gap-2 px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
            >
              <Search size={20} />
              <span className="hidden sm:inline">Search</span>
            </button>

            {user && (
              <span className="hidden sm:inline text-sm text-gray-500 px-2">
                {user.username}
              </span>
            )}

            <button
              onClick={handleLogout}
              className="flex items-center gap-2 px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
              title="Log out"
            >
              <LogOut size={20} />
              <span className="hidden sm:inline">Log out</span>
            </button>
          </div>
        </div>
      </div>
    </nav>
  );
}
