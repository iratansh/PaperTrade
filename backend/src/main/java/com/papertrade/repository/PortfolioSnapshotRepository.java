package com.papertrade.repository;

import com.papertrade.domain.PortfolioSnapshot;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface PortfolioSnapshotRepository extends R2dbcRepository<PortfolioSnapshot, UUID> {

    /**
     * All snapshots for an account, oldest first (chart-ready).
     */
    Flux<PortfolioSnapshot> findByAccountIdOrderByCapturedAtAsc(UUID accountId);

    /**
     * Snapshots for an account since a cutoff, oldest first.
     * Used to limit the chart to a recent window.
     */
    Flux<PortfolioSnapshot> findByAccountIdAndCapturedAtAfterOrderByCapturedAtAsc(
            UUID accountId, LocalDateTime after);
}
