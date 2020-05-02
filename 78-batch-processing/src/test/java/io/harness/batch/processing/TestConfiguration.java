package io.harness.batch.processing;

import static org.springframework.test.util.ReflectionTestUtils.getField;

import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.factory.ClosingFactory;
import io.harness.mongo.HObjectFactory;
import io.harness.mongo.QueryFactory;
import io.harness.persistence.HPersistence;
import io.harness.testlib.module.TestMongoModule;
import io.harness.testlib.rule.MongoRuleMixin;
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
  Morphia morphia() {
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new HObjectFactory());
    return morphia;
  }
  @Bean
  TestMongoModule testMongoModule(ClosingFactory closingFactory, Morphia morphia) {
    AdvancedDatastore primaryDatastore =
        (AdvancedDatastore) morphia.createDatastore(fakeMongoClient(closingFactory), databaseName());
    primaryDatastore.setQueryFactory(new QueryFactory());
    return new TestMongoModule(primaryDatastore);
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
