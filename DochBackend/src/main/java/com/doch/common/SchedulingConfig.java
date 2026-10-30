package com.doch.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables the background jobs (payment reconciliation, unpaid order expiry) and asynchronous e-mail sending. */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {
}