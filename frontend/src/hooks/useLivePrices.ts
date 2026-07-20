import { useEffect, useRef, useState } from 'react';
import type { PriceUpdate } from '../types';

/**
 * Subscribe to the live SSE price feed for a set of symbols.
 * Returns a map of symbol -> latest price, updated as ticks arrive.
 *
 * EventSource reconnects automatically on transient errors. The connection is
 * re-established whenever the set of symbols changes.
 */
export function useLivePrices(symbols: string[]): Record<string, number> {
  const [prices, setPrices] = useState<Record<string, number>>({});
  // Stable key so the effect only re-runs when the actual symbol set changes
  const key = [...symbols].sort().join(',');
  const esRef = useRef<EventSource | null>(null);

  useEffect(() => {
    if (!key) {
      setPrices({});
      return;
    }

    const es = new EventSource(`/api/stream/prices?symbols=${encodeURIComponent(key)}`);
    esRef.current = es;

    es.addEventListener('price', (event) => {
      try {
        const update = JSON.parse((event as MessageEvent).data) as PriceUpdate;
        setPrices((prev) => ({ ...prev, [update.symbol]: update.price }));
      } catch {
        /* ignore malformed tick */
      }
    });

    es.onerror = () => {
      // Browser auto-reconnects; nothing to do. Market-closed = no ticks (expected).
    };

    return () => {
      es.close();
      esRef.current = null;
    };
  }, [key]);

  return prices;
}
