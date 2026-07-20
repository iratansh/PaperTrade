package com.papertrade.controller;

import com.papertrade.domain.Transaction;
import com.papertrade.dto.TransactionResponse;
import com.papertrade.exception.AccountNotFoundException;
import com.papertrade.repository.TransactionRepository;
import com.papertrade.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Transaction history (executed fills) for the authenticated user's account.
 */
@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    @GetMapping
    public Flux<TransactionResponse> getTransactions(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return accountService.getAccountByUserId(userId)
            .switchIfEmpty(reactor.core.publisher.Mono.error(
                new AccountNotFoundException("No account for user: " + userId)))
            .flatMapMany(account ->
                transactionRepository.findByAccountIdOrderByTimestampDesc(account.getAccountId()))
            .map(this::toResponse);
    }

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
            t.getTransactionId().toString(),
            t.getSymbol(),
            t.getType(),
            t.getQuantity(),
            t.getPrice(),
            t.getFees(),
            t.getTotalValue(),
            t.getTimestamp().toString()
        );
    }
}
