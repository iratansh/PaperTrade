# PaperTrade Frontend

React + TypeScript frontend for the PaperTrade application.

## Features

- 📊 Dashboard with portfolio overview
- 📈 Chart.js visualization of portfolio growth
- 🔍 Stock search functionality
- 💹 Buy/Sell stocks (Market and Limit orders)
- 📱 Responsive, minimalist design

## Tech Stack

- **React 18** - UI framework
- **TypeScript** - Type safety
- **Vite** - Build tool
- **Tailwind CSS** - Styling
- **Chart.js** - Portfolio charts
- **React Router** - Navigation
- **Axios** - API calls

## Getting Started

### Install Dependencies

```bash
npm install
```

### Run Development Server

```bash
npm run dev
```

Frontend runs on `http://localhost:3000`

### Build for Production

```bash
npm run build
```

## Project Structure

```
src/
├── components/       # Reusable UI components
│   ├── Navbar.tsx
│   ├── PortfolioChart.tsx
│   └── PositionCard.tsx
├── pages/           # Page components
│   ├── Dashboard.tsx
│   ├── StockDetail.tsx
│   └── Search.tsx
├── services/        # API service layer
│   └── api.ts
├── types/           # TypeScript interfaces
│   └── index.ts
├── utils/           # Utility functions
│   └── format.ts
├── App.tsx          # Main app with routing
└── main.tsx         # Entry point
```

## Environment Variables

Create a `.env` file (optional):

```bash
VITE_API_BASE_URL=http://localhost:8080
```

Note: Vite proxy is configured in `vite.config.ts` for local development.

## API Integration

All API calls go through the `services/api.ts` layer:

- `portfolioApi.getPortfolio(userId)`
- `ordersApi.placeOrder(request)`
- `marketDataApi.getQuote(symbol)`

## Temporary Auth

Currently uses hardcoded user/account IDs:
- User ID: `123e4567-e89b-12d3-a456-426614174000`
- Account ID: `223e4567-e89b-12d3-a456-426614174000`

JWT authentication will be added in the next iteration.
