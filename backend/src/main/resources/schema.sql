-- Users table
CREATE TABLE IF NOT EXISTS users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    enabled BOOLEAN NOT NULL DEFAULT true
);

-- Trading accounts table
CREATE TABLE IF NOT EXISTS accounts (
    account_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    initial_balance NUMERIC(15, 2) NOT NULL DEFAULT 100000.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id) -- One account per user
);

-- Positions table (current holdings)
CREATE TABLE IF NOT EXISTS positions (
    position_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(account_id) ON DELETE CASCADE,
    symbol VARCHAR(10) NOT NULL,
    quantity NUMERIC(15, 4) NOT NULL,
    average_cost NUMERIC(15, 2) NOT NULL,
    current_price NUMERIC(15, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(account_id, symbol) -- One position per symbol per account
);

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(account_id) ON DELETE CASCADE,
    symbol VARCHAR(10) NOT NULL,
    type VARCHAR(20) NOT NULL, -- MARKET, LIMIT
    side VARCHAR(10) NOT NULL, -- BUY, SELL
    quantity NUMERIC(15, 4) NOT NULL,
    limit_price NUMERIC(15, 2),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, FILLED, CANCELLED, REJECTED
    filled_price NUMERIC(15, 2),
    filled_quantity NUMERIC(15, 4),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    filled_at TIMESTAMP,
    idempotency_key VARCHAR(255) UNIQUE,
    CONSTRAINT check_limit_price CHECK (type != 'LIMIT' OR limit_price IS NOT NULL)
);

-- Transactions table (immutable audit log)
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(order_id),
    account_id UUID NOT NULL REFERENCES accounts(account_id) ON DELETE CASCADE,
    symbol VARCHAR(10) NOT NULL,
    type VARCHAR(10) NOT NULL, -- BUY, SELL
    quantity NUMERIC(15, 4) NOT NULL,
    price NUMERIC(15, 2) NOT NULL,
    fees NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Watchlist table (for price alerts)
CREATE TABLE IF NOT EXISTS watchlist (
    watchlist_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    symbol VARCHAR(10) NOT NULL,
    alert_price NUMERIC(15, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, symbol)
);

-- Notifications table
CREATE TABLE IF NOT EXISTS notifications (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_positions_account_id ON positions(account_id);
CREATE INDEX idx_orders_account_id ON orders(account_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_timestamp ON transactions(timestamp);
CREATE INDEX idx_watchlist_user_id ON watchlist(user_id);
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
