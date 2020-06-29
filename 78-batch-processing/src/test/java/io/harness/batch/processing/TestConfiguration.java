package io.harness.batch.processing;

import static org.springframework.test.util.ReflectionTestUtils.getField;

import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.factory.ClosingFactory;
import io.harness.mongo.QueryFactory;
import io.harness.morphia.MorphiaModule;
import io.harness.persistence.HPersistence;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.testlib.module.TestMongoModule;
import lombok.val;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;

import java.util.Map;

@Configuration
@Profile("test")
public class TestConfiguration implements MongoRuleMixin {
  @Bean
  MorphiaModule morphiaModule() {
    return new MorphiaModule(true);
  }

  @Bean
  MongoRuleMixin.MongoType mongoType() {
    return MongoType.FAKE;
  }

  @Bean
  TestMongoModule testMongoModule() {
    return new TestMongoModule();
  }

  @Bean
  public MongoDbFactory mongoDbFactory(
      ClosingFactory closingFactory, HPersistence hPersistence, BatchMainConfig config, Morphia morphia) {
    AdvancedDatastore eventsDatastore =
        (AdvancedDatastore) morphia.createDatastore(fakeMongoClient(closingFactory), "events");
    eventsDatastore.setQueryFactory(new QueryFactory());

    @SuppressWarnings("unchecked")
    val datastoreMap = (Map<String, AdvancedDatastore>) getField(hPersistence, "datastoreMap");
    datastoreMap.put("events", eventsDatastore);

    return new SimpleMongoDbFactory(eventsDatastore.getMongo(), eventsDatastore.getDB().getName());
  }

  @Bean
  ClosingFactory closingFactory() {
    return new ClosingFactory();
  }
}
