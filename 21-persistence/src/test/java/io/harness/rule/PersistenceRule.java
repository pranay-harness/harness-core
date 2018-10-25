package io.harness.rule;

import static java.util.Arrays.asList;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.mongodb.MongoClient;
import io.harness.factory.ClosingFactory;
import io.harness.mongo.MongoModule;
import io.harness.mongo.MongoPersistence;
import io.harness.mongo.NoDefaultConstructorMorphiaObjectFactory;
import io.harness.mongo.QueryFactory;
import io.harness.persistence.HPersistence;
import io.harness.time.TimeModule;
import io.harness.version.VersionModule;
import lombok.Getter;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PersistenceRule implements MethodRule, InjectorRuleMixin, MongoRuleMixin, DistributedLockRuleMixin {
  private static final Logger logger = LoggerFactory.getLogger(PersistenceRule.class);

  @Getter private AdvancedDatastore datastore;

  public List<Module> modules(ClosingFactory closingFactory) {
    String databaseName = databaseName();
    MongoClient mongoClient = fakeMongoClient(0, closingFactory);

    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new NoDefaultConstructorMorphiaObjectFactory());
    datastore = (AdvancedDatastore) morphia.createDatastore(mongoClient, databaseName);
    datastore.setQueryFactory(new QueryFactory());

    DistributedLockSvc distributedLockSvc = distributedLockSvc(mongoClient, databaseName, closingFactory);

    Module dummyMongo = new AbstractModule() {
      @Override
      protected void configure() {
        bind(HPersistence.class).to(MongoPersistence.class);
      }
    };

    return asList(
        new VersionModule(), new TimeModule(), new MongoModule(datastore, datastore, distributedLockSvc), dummyMongo);
  }
  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return applyInjector(statement, target);
  }
}
