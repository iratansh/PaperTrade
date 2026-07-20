import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { ordersApi, transactionsApi } from '../services/api';
import { formatCurrency, formatDate } from '../utils/format';
import type { Order, TransactionRecord } from '../types';

const STATUS_STYLES: Record<Order['status'], string> = {
  FILLED: 'bg-green-100 text-green-700',
  PENDING: 'bg-yellow-100 text-yellow-700',
  CANCELLED: 'bg-gray-100 text-gray-600',
  REJECTED: 'bg-red-100 text-red-700',
};

export default function History() {
  const navigate = useNavigate();
  const [orders, setOrders] = useState<Order[]>([]);
  const [transactions, setTransactions] = useState<TransactionRecord[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const [o, t] = await Promise.all([
          ordersApi.getOrderHistory(),
          transactionsApi.list(),
        ]);
        setOrders(o);
        setTransactions(t);
      } catch (err) {
        console.error('Failed to load history:', err);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

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

        <h1 className="text-2xl font-bold text-gray-900 mb-6">Activity</h1>

        {loading ? (
          <div className="flex justify-center py-12">
            <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600" />
          </div>
        ) : (
          <div className="space-y-6">
            {/* Orders */}
            <section className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
              <div className="px-6 py-4 border-b border-gray-100">
                <h2 className="font-semibold text-gray-900">Orders</h2>
              </div>
              {orders.length === 0 ? (
                <p className="px-6 py-8 text-center text-gray-500">No orders yet.</p>
              ) : (
                <ul className="divide-y divide-gray-100">
                  {orders.map((o) => (
                    <li key={o.orderId} className="px-6 py-4 flex items-center justify-between">
                      <div>
                        <p className="font-semibold text-gray-900">
                          <span className={o.side === 'BUY' ? 'text-success' : 'text-danger'}>
                            {o.side}
                          </span>{' '}
                          {o.quantity} {o.symbol}
                        </p>
                        <p className="text-sm text-gray-500">
                          {o.type}
                          {o.limitPrice ? ` @ ${formatCurrency(o.limitPrice)}` : ''} ·{' '}
                          {formatDate(o.createdAt)}
                        </p>
                      </div>
                      <div className="text-right">
                        <span className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${STATUS_STYLES[o.status]}`}>
                          {o.status}
                        </span>
                        {o.filledPrice != null && (
                          <p className="text-sm text-gray-600 mt-1">
                            {formatCurrency(o.filledPrice)}
                          </p>
                        )}
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            {/* Transactions */}
            <section className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
              <div className="px-6 py-4 border-b border-gray-100">
                <h2 className="font-semibold text-gray-900">Transactions</h2>
              </div>
              {transactions.length === 0 ? (
                <p className="px-6 py-8 text-center text-gray-500">No transactions yet.</p>
              ) : (
                <ul className="divide-y divide-gray-100">
                  {transactions.map((t) => (
                    <li key={t.transactionId} className="px-6 py-4 flex items-center justify-between">
                      <div>
                        <p className="font-semibold text-gray-900">
                          <span className={t.type === 'BUY' ? 'text-success' : 'text-danger'}>
                            {t.type}
                          </span>{' '}
                          {t.quantity} {t.symbol}
                        </p>
                        <p className="text-sm text-gray-500">
                          {formatCurrency(t.price)} · {formatDate(t.timestamp)}
                        </p>
                      </div>
                      <p className={`font-semibold ${t.type === 'BUY' ? 'text-danger' : 'text-success'}`}>
                        {t.type === 'BUY' ? '-' : '+'}{formatCurrency(t.totalValue)}
                      </p>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>
        )}
      </div>
    </div>
  );
}
