Object-Oriented Design:


User stories:

User can log into the applicatioin

User gets brought into wealthsimple-like dashboard:  Dashboard displays Users balance, a chart.js visualisation on their portfolio growth, and users stocks & amount of money in each stock

User can click on the stock row and will get taken to a stock info page which has options to buy or sell the stock along with another chart.js vis for the stock price history

Dashboard navbar has a search button: takes user to a search input page where user can search for a specific stock ticker

User can then buy & sell stock from this page.

Users start with 100k base paper trade balance- it's on the user to manage their money correctly, if they invest in poor trades and hit 0 then their account becomes useless in terms of buying/selling. 

Transactions need to be validated- no transactions can occur that would reduce the user's balance below 0.

Users can track stocks and recieve alerts based off of their notification preferences.

Allow for extension of the application to support options trading.


Requirements:

- Minimalist UI
- JWT based Authentication
- Backend focused application


Tech stack:

Java (Spring Webflux) backend
Websocket (Finnhub listener for stock data)

Typescript & React + Chart.js Frontend

PostgreSQL + AWS RDS 

AWS ALB, ECS Fargate, ECR Infrastructure

Elasticache for stock data

Terraform IaC


Required Tables:

Users: UUID (PK), username, name, email, password (encrypted)

Portfolio: username (FK), balance, stocks & amount of each stock

Notifications: username (FK), user pref for alerts

Logs: username(FK), Transaction (object)


OBJECT-ORIENTED DESIGN (for interview):

Core Domain Classes:
- User: Represents authenticated user
- TradingAccount: Manages balance, positions, portfolio value
- Position: Individual stock holding (symbol, qty, avg cost, P&L)
- Order: Buy/Sell order with lifecycle (pending → filled/cancelled)
- Transaction: Immutable record of executed trades
- MarketData: Real-time price quotes

Design Patterns:
1. Strategy Pattern: OrderExecutionStrategy (Market vs Limit vs Options)
2. Observer Pattern: MarketDataObserver for price updates
3. Repository Pattern: Data access abstraction
4. Factory Pattern: OrderFactory for different order types

SOLID Principles:
- Single Responsibility: OrderValidator, OrderExecutor, BalanceManager separate
- Open/Closed: Abstract Security class → Stock, Option (future)
- Liskov Substitution: All OrderExecutionStrategy implementations interchangeable
- Interface Segregation: MarketDataProvider interface
- Dependency Inversion: Depend on interfaces, not concrete classes

Key Architectural Decisions:
- Reactive programming (Spring WebFlux) for scalability
- Database-level locking for balance updates (prevent race conditions)
- Cache-aside pattern for market data (5s TTL in Redis)
- Event-driven order processing for reliability
- Idempotency keys to prevent duplicate orders

Concurrency Handling:
- Optimistic locking (@Version) on TradingAccount
- Database SERIALIZABLE isolation for critical transactions
- Thread-safe observers (CopyOnWriteArrayList)

Performance Optimizations:
- Redis caching for stock quotes (reduce Finnhub API calls)
- Read replicas for portfolio queries
- WebSocket connection pooling
- Lazy loading of position history

Extension for Options Trading:
- Abstract Security class allows Option subclass
- OptionsOrderStrategy implements OrderExecutionStrategy
- No modification to existing code (Open/Closed principle)
