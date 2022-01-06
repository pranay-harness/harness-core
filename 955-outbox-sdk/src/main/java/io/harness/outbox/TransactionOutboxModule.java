/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.outbox.OutboxSDKConstants.DEFAULT_OUTBOX_POLL_CONFIGURATION;

import io.harness.annotations.dev.OwnedBy;
import io.harness.metrics.jobs.RecordMetricsJob;
import io.harness.metrics.modules.MetricsModule;
import io.harness.metrics.service.api.MetricService;
import io.harness.metrics.service.api.MetricsPublisher;
import io.harness.mongo.MongoConfig;
import io.harness.outbox.api.OutboxDao;
import io.harness.outbox.api.OutboxService;
import io.harness.outbox.api.impl.OutboxDaoImpl;
import io.harness.outbox.api.impl.OutboxServiceImpl;
import io.harness.outbox.monitor.OutboxMetricsPublisher;
import io.harness.persistence.HPersistence;
import io.harness.springdata.HTransactionTemplate;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(PL)
@Slf4j
public class TransactionOutboxModule extends AbstractModule {
  public static final String OUTBOX_TRANSACTION_TEMPLATE = "OUTBOX_TRANSACTION_TEMPLATE";
  public static final String SERVICE_ID_FOR_OUTBOX = "serviceIdForOutboxMetrics";
  private final OutboxPollConfiguration outboxPollConfiguration;
  private final String serviceId;
  private final boolean exportMetricsToStackDriver;

  public TransactionOutboxModule(
      OutboxPollConfiguration outboxPollConfiguration, @NotNull String serviceId, boolean exportMetricsToStackDriver) {
    if (outboxPollConfiguration == null) {
      outboxPollConfiguration = DEFAULT_OUTBOX_POLL_CONFIGURATION;
    }
    if (outboxPollConfiguration.getLockId() == null) {
      outboxPollConfiguration.setLockId(serviceId);
    }
    this.outboxPollConfiguration = outboxPollConfiguration;
    this.serviceId = serviceId;
    this.exportMetricsToStackDriver = exportMetricsToStackDriver;
  }

  @Override
  protected void configure() {
    registerRequiredBindings();
    bind(OutboxDao.class).to(OutboxDaoImpl.class);
    bind(OutboxService.class).to(OutboxServiceImpl.class);
    if (exportMetricsToStackDriver) {
      bind(MetricsPublisher.class)
          .annotatedWith(Names.named("OutboxMetricsPublisher"))
          .to(OutboxMetricsPublisher.class)
          .in(Scopes.SINGLETON);
    } else {
      log.info("No configuration provided for Stack Driver, metrics will not be recorded.");
    }
  }

  @Provides
  @Singleton
  public OutboxPollConfiguration getOutboxPollConfiguration() {
    return this.outboxPollConfiguration;
  }

  @Provides
  @Singleton
  @Named(OUTBOX_TRANSACTION_TEMPLATE)
  protected TransactionTemplate getTransactionTemplate(
      MongoTransactionManager mongoTransactionManager, MongoConfig mongoConfig) {
    return new HTransactionTemplate(mongoTransactionManager, mongoConfig.isTransactionsEnabled());
  }

  @Provides
  @Singleton
  @Named(SERVICE_ID_FOR_OUTBOX)
  public String getServiceIdForOutboxMetrics() {
    return serviceId;
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
    if (exportMetricsToStackDriver) {
      requireBinding(MetricsModule.class);
      requireBinding(MetricService.class);
      requireBinding(RecordMetricsJob.class);
    }
  }
}
