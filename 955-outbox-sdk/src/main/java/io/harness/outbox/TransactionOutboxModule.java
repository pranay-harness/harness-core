package io.harness.outbox;

import static io.harness.outbox.OutboxSDKConstants.OUTBOX_TRANSACTION_TEMPLATE;

import io.harness.mongo.MongoConfig;
import io.harness.outbox.api.OutboxEventService;
import io.harness.outbox.api.impl.OutboxEventServiceImpl;
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
  @Override
  protected void configure() {
    bind(OutboxEventService.class).to(OutboxEventServiceImpl.class);
  }

  @Provides
  @Singleton
  @Named(OUTBOX_TRANSACTION_TEMPLATE)
  protected TransactionTemplate getTransactionTemplate(
      MongoTransactionManager mongoTransactionManager, MongoConfig mongoConfig) {
    return new HTransactionTemplate(mongoTransactionManager, mongoConfig.isTransactionsEnabled());
  }
}
