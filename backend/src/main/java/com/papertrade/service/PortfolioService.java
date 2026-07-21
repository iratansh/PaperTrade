package com.papertrade.service;

import com.papertrade.domain.Position;
import com.papertrade.domain.TradingAccount;
import com.papertrade.dto.PortfolioResponse;
import com.papertrade.dto.PortfolioResponse.PositionResponse;
import com.papertrade.exception.AccountNotFoundException;
import com.papertrade.repository.AccountRepository;
import com.papertrade.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final MarketDataService marketDataService;

    /**
     * Get complete portfolio summary for a user
     *
     * Includes:
     * - Cash balance
     * - All positions with P&L (valued at the CURRENT market price)
     * - Total portfolio value
     * - Aggregate P&L
     */
    public Mono<PortfolioResponse> getPortfolio(UUID userId) {
        return accountRepository.findByUserId(userId)
            .switchIfEmpty(Mono.error(new AccountNotFoundException("Account not found for user: " + userId)))
            .flatMap(account ->
                // Refresh each position's price before valuing it. position.currentPrice
                // is only written at buy time, so without this the whole portfolio would
                // be stuck at cost basis. getCurrentPrice is Redis-cached, so this is cheap.
                positionRepository.findByAccountId(account.getAccountId())
                    .flatMapSequential(this::withCurrentPrice)
                    .collectList()
                    .map(positions -> buildResponse(account, positions)));
    }

    /**
     * Overlay the latest market price onto a position (keeping its last known
     * price if the quote lookup fails).
     */
    private Mono<Position> withCurrentPrice(Position position) {
        return marketDataService.getCurrentPrice(position.getSymbol())
            .map(price -> {
                position.setCurrentPrice(price);
                return position;
            })
            .onErrorReturn(position);
    }

    private PortfolioResponse buildResponse(TradingAccount account, List<Position> positions) {
        List<PositionResponse> positionResponses = positions.stream()
            .map(this::toPositionResponse)
            .toList();

        BigDecimal totalPositionValue = positions.stream()
            .map(Position::getCurrentValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGainLoss = positions.stream()
            .map(Position::getUnrealizedPnL)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPortfolioValue = account.getBalance().add(totalPositionValue);

        // Gain/loss % is measured against the amount actually invested (cost basis)
        BigDecimal totalCostBasis = account.getInitialBalance().subtract(account.getBalance());
        BigDecimal totalGainLossPercentage = BigDecimal.ZERO;
        if (totalCostBasis.compareTo(BigDecimal.ZERO) > 0) {
            totalGainLossPercentage = totalGainLoss
                .divide(totalCostBasis, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
        }

        return PortfolioResponse.builder()
            .cashBalance(account.getBalance())
            .totalPositionValue(totalPositionValue)
            .totalPortfolioValue(totalPortfolioValue)
            .totalGainLoss(totalGainLoss)
            .totalGainLossPercentage(totalGainLossPercentage)
            .positions(positionResponses)
            .build();
    }

    /**
     * Convert Position entity to PositionResponse DTO
     */
    private PositionResponse toPositionResponse(Position position) {
        return PositionResponse.builder()
            .symbol(position.getSymbol())
            .quantity(position.getQuantity())
            .averageCost(position.getAverageCost())
            .currentPrice(position.getCurrentPrice())
            .currentValue(position.getCurrentValue())
            .unrealizedPnL(position.getUnrealizedPnL())
            .unrealizedPnLPercentage(position.getUnrealizedPnLPercentage())
            .build();
    }
}
