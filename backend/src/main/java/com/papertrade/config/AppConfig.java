package com.papertrade.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * General application configuration
 */
@Configuration
@EnableTransactionManagement
public class AppConfig {

    /**
     * Programmatic reactive transaction operator.
     * Lets us wrap ONLY the database work in a transaction, keeping external
     * calls (e.g. fetching market prices) outside the transaction boundary.
     */
    @Bean
    public TransactionalOperator transactionalOperator(ReactiveTransactionManager tm) {
        return TransactionalOperator.create(tm);
    }
}
