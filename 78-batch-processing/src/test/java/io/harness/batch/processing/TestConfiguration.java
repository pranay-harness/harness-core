package io.harness.batch.processing;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import io.harness.factory.ClosingFactory;
import io.harness.module.TestMongoModule;
import io.harness.mongo.HObjectFactory;
import io.harness.mongo.QueryFactory;
import io.harness.rule.MongoRuleMixin;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestConfiguration implements MongoRuleMixin {
  @Bean
  TestMongoModule testMongoModule(ClosingFactory closingFactory) {
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new HObjectFactory());
    AdvancedDatastore primaryDatastore =
        (AdvancedDatastore) morphia.createDatastore(fakeMongoClient(closingFactory), databaseName());
    primaryDatastore.setQueryFactory(new QueryFactory());
    DistributedLockSvc distributedLockSvc = null;
    return new TestMongoModule(primaryDatastore, null);
  }

  @Bean
  ClosingFactory closingFactory() {
    return new ClosingFactory();
  }
}
