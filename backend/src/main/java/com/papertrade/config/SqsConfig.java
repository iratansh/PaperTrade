package com.papertrade.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * SQS client for the cloud order queue. Only created under the "aws" profile.
 *
 * Credentials come from the default provider chain (the ECS task role in AWS),
 * so no keys live in the app.
 */
@Configuration
@Profile("aws")
public class SqsConfig {

    @Bean
    public SqsAsyncClient sqsAsyncClient(
            @org.springframework.beans.factory.annotation.Value("${aws.region:us-east-1}") String region) {
        return SqsAsyncClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }
}
