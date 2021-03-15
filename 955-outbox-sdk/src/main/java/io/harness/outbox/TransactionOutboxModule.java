package io.harness.outbox;

import io.harness.mongo.MongoConfig;
import io.harness.outbox.api.OutboxService;
import io.harness.outbox.api.impl.OutboxServiceImpl;
import io.harness.persistence.HPersistence;
import io.harness.springdata.HTransactionTemplate;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public class TransactionOutboxModule extends AbstractModule {
  public static final String OUTBOX_TRANSACTION_TEMPLATE = "OUTBOX_TRANSACTION_TEMPLATE";

  @Override
  protected void configure() {
    registerRequiredBindings();
    bind(OutboxService.class).to(OutboxServiceImpl.class);
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
  }

  @Provides
  @Singleton
  @Named(OUTBOX_TRANSACTION_TEMPLATE)
  protected TransactionTemplate getTransactionTemplate(
      MongoTransactionManager mongoTransactionManager, MongoConfig mongoConfig) {
    return new HTransactionTemplate(mongoTransactionManager, mongoConfig.isTransactionsEnabled());
  }
}
