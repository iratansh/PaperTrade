import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import ProtectedRoute from './components/ProtectedRoute';
import Navbar from './components/Navbar';
import Dashboard from './pages/Dashboard';
import StockDetail from './pages/StockDetail';
import Search from './pages/Search';
import History from './pages/History';
import Login from './pages/Login';
import Register from './pages/Register';

/**
 * Authenticated app shell (navbar + protected pages).
 */
function AppShell() {
  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/stock/:symbol" element={<StockDetail />} />
        <Route path="/search" element={<Search />} />
        <Route path="/history" element={<History />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </>
  );
}

function AppRoutes() {
  const { isAuthenticated } = useAuth();

  return (
    <Routes>
      <Route
        path="/login"
        element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <Login />}
      />
      <Route
        path="/register"
        element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <Register />}
      />
      <Route
        path="/*"
        element={
          <ProtectedRoute>
            <AppShell />
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <div className="min-h-screen bg-gray-50">
            <AppRoutes />
          </div>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
