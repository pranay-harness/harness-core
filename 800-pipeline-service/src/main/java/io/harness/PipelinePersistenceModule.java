package io.harness;

import io.harness.mongo.MongoConfig;
import io.harness.notification.NotificationChannelPersistenceConfig;
import io.harness.springdata.HTransactionTemplate;
import io.harness.springdata.SpringPersistenceConfig;
import io.harness.springdata.SpringPersistenceModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class PipelinePersistenceModule extends SpringPersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class[] {SpringPersistenceConfig.class, NotificationChannelPersistenceConfig.class};
  }

  @Provides
  @Singleton
  protected TransactionTemplate getTransactionTemplate(
      MongoTransactionManager mongoTransactionManager, MongoConfig mongoConfig) {
    return new HTransactionTemplate(mongoTransactionManager, mongoConfig.isTransactionsEnabled());
  }
}
