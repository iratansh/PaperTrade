import { createContext, useContext, useState, ReactNode } from 'react';
import { authApi, tokenStore } from '../services/api';

interface AuthUser {
  userId: string;
  username: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => tokenStore.getUser());

  const login = async (username: string, password: string) => {
    const auth = await authApi.login(username, password);
    tokenStore.save(auth);
    setUser({ userId: auth.userId, username: auth.username });
  };

  const register = async (username: string, email: string, password: string) => {
    const auth = await authApi.register(username, email, password);
    tokenStore.save(auth);
    setUser({ userId: auth.userId, username: auth.username });
  };

  const logout = () => {
    tokenStore.clear();
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{ user, isAuthenticated: !!user, login, register, logout }}
    >
      {children}
    </AuthContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
