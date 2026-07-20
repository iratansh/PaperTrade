import { useEffect, useRef, useState } from 'react';
import { Bell } from 'lucide-react';
import { notificationsApi } from '../services/api';
import { formatDate } from '../utils/format';
import type { AppNotification } from '../types';

export default function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const [items, setItems] = useState<AppNotification[]>([]);
  const ref = useRef<HTMLDivElement>(null);

  // Poll unread count
  useEffect(() => {
    let active = true;
    const poll = async () => {
      try {
        const count = await notificationsApi.unreadCount();
        if (active) setUnread(count);
      } catch {
        /* ignore */
      }
    };
    poll();
    const id = setInterval(poll, 30000);
    return () => {
      active = false;
      clearInterval(id);
    };
  }, []);

  // Close on outside click
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const toggle = async () => {
    const next = !open;
    setOpen(next);
    if (next) {
      try {
        const list = await notificationsApi.list();
        setItems(list);
        if (unread > 0) {
          await notificationsApi.markAllRead();
          setUnread(0);
        }
      } catch {
        /* ignore */
      }
    }
  };

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={toggle}
        className="relative flex items-center px-3 py-2 text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
        title="Notifications"
      >
        <Bell size={20} />
        {unread > 0 && (
          <span className="absolute top-1 right-1 min-w-[18px] h-[18px] px-1 bg-danger text-white text-xs font-bold rounded-full flex items-center justify-center">
            {unread > 9 ? '9+' : unread}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 mt-2 w-80 bg-white rounded-xl shadow-lg border border-gray-200 overflow-hidden z-50">
          <div className="px-4 py-3 border-b border-gray-100 font-semibold text-gray-900">
            Notifications
          </div>
          <div className="max-h-96 overflow-y-auto">
            {items.length === 0 ? (
              <p className="px-4 py-8 text-center text-gray-500 text-sm">No notifications yet.</p>
            ) : (
              <ul className="divide-y divide-gray-100">
                {items.map((n) => (
                  <li key={n.notificationId} className="px-4 py-3">
                    <p className="text-sm text-gray-900">{n.message}</p>
                    <p className="text-xs text-gray-400 mt-1">{formatDate(n.createdAt)}</p>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
