package com.papertrade.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

/**
 * R2DBC configuration for reactive database access
 *
 * Features:
 * - Auto-initializes database schema from schema.sql
 * - Enables reactive repositories
 */
@Configuration
public class R2dbcConfig extends AbstractR2dbcConfiguration {

    private final ConnectionFactory connectionFactory;

    public R2dbcConfig(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public ConnectionFactory connectionFactory() {
        return connectionFactory;
    }

    /**
     * Initialize database schema on startup
     * Disabled - run schema.sql manually instead
     */
    // @Bean
    // public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
    //     ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
    //     initializer.setConnectionFactory(connectionFactory);

    //     ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    //     populator.addScript(new ClassPathResource("schema.sql"));

    //     initializer.setDatabasePopulator(populator);
    //     return initializer;
    // }
}
