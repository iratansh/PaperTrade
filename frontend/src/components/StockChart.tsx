import { Line } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Tooltip,
  Filler,
} from 'chart.js';
import type { Candle } from '../types';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Filler);

interface StockChartProps {
  candles: Candle[];
}

export default function StockChart({ candles }: StockChartProps) {
  if (candles.length === 0) {
    return (
      <div className="h-64 flex items-center justify-center text-gray-400">
        No price data available
      </div>
    );
  }

  const closes = candles.map((c) => c.close);
  const labels = candles.map((c) => c.datetime);
  const isPositive = closes[closes.length - 1] >= closes[0];

  const data = {
    labels,
    datasets: [
      {
        data: closes,
        borderColor: isPositive ? '#10b981' : '#ef4444',
        backgroundColor: isPositive ? 'rgba(16,185,129,0.1)' : 'rgba(239,68,68,0.1)',
        fill: true,
        tension: 0.3,
        pointRadius: 0,
        pointHoverRadius: 5,
        borderWidth: 2,
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        mode: 'index' as const,
        intersect: false,
        backgroundColor: '#1f2937',
        padding: 12,
        displayColors: false,
        callbacks: {
          label: (ctx: any) =>
            `$${ctx.parsed.y.toLocaleString('en-US', {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2,
            })}`,
        },
      },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { maxTicksLimit: 6, maxRotation: 0 },
      },
      y: {
        grid: { color: '#f3f4f6' },
        ticks: { callback: (v: any) => `$${v}` },
      },
    },
    interaction: { mode: 'nearest' as const, axis: 'x' as const, intersect: false },
  };

  return (
    <div className="h-64 w-full">
      <Line data={data} options={options} />
    </div>
  );
}
