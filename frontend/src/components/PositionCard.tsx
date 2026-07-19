import { TrendingUp, TrendingDown } from 'lucide-react';
import { formatCurrency, formatPercent } from '../utils/format';
import type { Position } from '../types';

interface PositionCardProps {
  position: Position;
  onClick: (symbol: string) => void;
}

export default function PositionCard({ position, onClick }: PositionCardProps) {
  const isPositive = position.unrealizedPnL >= 0;

  return (
    <div
      onClick={() => onClick(position.symbol)}
      className="bg-white rounded-lg border border-gray-200 p-4 hover:shadow-md transition-shadow cursor-pointer"
    >
      <div className="flex justify-between items-start mb-2">
        <div>
          <h3 className="text-lg font-semibold text-gray-900">{position.symbol}</h3>
          <p className="text-sm text-gray-500">
            {position.quantity} shares @ {formatCurrency(position.averageCost)}
          </p>
        </div>
        <div className={`flex items-center gap-1 ${isPositive ? 'text-success' : 'text-danger'}`}>
          {isPositive ? <TrendingUp size={20} /> : <TrendingDown size={20} />}
          <span className="font-semibold">{formatPercent(position.unrealizedPnLPercentage)}</span>
        </div>
      </div>

      <div className="flex justify-between items-end">
        <div>
          <p className="text-2xl font-bold text-gray-900">
            {formatCurrency(position.currentValue)}
          </p>
          <p className={`text-sm font-medium ${isPositive ? 'text-success' : 'text-danger'}`}>
            {isPositive ? '+' : ''}{formatCurrency(position.unrealizedPnL)}
          </p>
        </div>
        <div className="text-right">
          <p className="text-sm text-gray-500">Current Price</p>
          <p className="text-lg font-semibold text-gray-900">
            {formatCurrency(position.currentPrice)}
          </p>
        </div>
      </div>
    </div>
  );
}
