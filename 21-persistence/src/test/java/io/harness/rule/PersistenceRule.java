package io.harness.rule;

import static java.util.Arrays.asList;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import io.harness.factory.ClosingFactory;
import io.harness.mongo.HObjectFactory;
import io.harness.mongo.MongoModule;
import io.harness.mongo.MongoPersistence;
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

import java.lang.annotation.Annotation;
import java.util.List;

public class PersistenceRule implements MethodRule, InjectorRuleMixin, MongoRuleMixin, DistributedLockRuleMixin {
  private static final Logger logger = LoggerFactory.getLogger(PersistenceRule.class);

  ClosingFactory closingFactory;
  @Getter private AdvancedDatastore datastore;

  public PersistenceRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) throws Exception {
    String databaseName = databaseName();
    MongoInfo mongoInfo = testMongo(annotations, closingFactory);

    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new HObjectFactory());
    datastore = (AdvancedDatastore) morphia.createDatastore(mongoInfo.getClient(), databaseName);
    datastore.setQueryFactory(new QueryFactory());

    DistributedLockSvc distributedLockSvc = distributedLockSvc(mongoInfo.getClient(), databaseName, closingFactory);

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
    return applyInjector(statement, frameworkMethod, target);
  }
}
