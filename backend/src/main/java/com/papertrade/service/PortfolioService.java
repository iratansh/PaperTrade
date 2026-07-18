package com.papertrade.service;

import com.papertrade.domain.Position;
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

    /**
     * Get complete portfolio summary for a user
     *
     * Includes:
     * - Cash balance
     * - All positions with P&L
     * - Total portfolio value
     * - Aggregate P&L
     */
    public Mono<PortfolioResponse> getPortfolio(UUID userId) {
        return accountRepository.findByUserId(userId)
            .switchIfEmpty(Mono.error(new AccountNotFoundException("Account not found for user: " + userId)))
            .flatMap(account -> {
                // Get all positions for this account
                return positionRepository.findByAccountId(account.getAccountId())
                    .collectList()
                    .map(positions -> {
                        // Convert positions to DTOs
                        List<PositionResponse> positionResponses = positions.stream()
                            .map(this::toPositionResponse)
                            .toList();

                        // Calculate totals
                        BigDecimal totalPositionValue = positions.stream()
                            .map(Position::getCurrentValue)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                        BigDecimal totalGainLoss = positions.stream()
                            .map(Position::getUnrealizedPnL)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                        BigDecimal totalPortfolioValue = account.getBalance().add(totalPositionValue);

                        // Calculate total gain/loss percentage
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
                    });
            });
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
