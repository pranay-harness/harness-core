package io.harness.rule;

import com.google.inject.Guice;
import com.google.inject.Injector;

import com.deftlabs.lock.mongo.DistributedLockSvcFactory;
import com.deftlabs.lock.mongo.DistributedLockSvcOptions;
import com.mongodb.MongoClient;
import io.harness.factory.ClosingFactory;
import io.harness.lock.ManagedDistributedLockSvc;
import io.harness.mongo.MongoModule;
import io.harness.mongo.NoDefaultConstructorMorphiaObjectFactory;
import io.harness.mongo.QueryFactory;
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

import java.io.Closeable;
import java.io.IOException;

public class PersistenceRule implements MethodRule, MongoRuleMixin {
  private static final Logger logger = LoggerFactory.getLogger(PersistenceRule.class);

  private Injector injector;

  private ClosingFactory closingFactory = new ClosingFactory();

  @Getter private AdvancedDatastore datastore;

  protected void before() {
    MongoClient mongoClient = fakeMongoClient(0, closingFactory);

    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new NoDefaultConstructorMorphiaObjectFactory());
    datastore = (AdvancedDatastore) morphia.createDatastore(mongoClient, databaseName());
    datastore.setQueryFactory(new QueryFactory());

    DistributedLockSvcOptions distributedLockSvcOptions =
        new DistributedLockSvcOptions(mongoClient, databaseName(), "locks");
    distributedLockSvcOptions.setEnableHistory(false);
    ManagedDistributedLockSvc distributedLockSvc =
        new ManagedDistributedLockSvc(new DistributedLockSvcFactory(distributedLockSvcOptions).getLockSvc());
    if (!distributedLockSvc.isRunning()) {
      distributedLockSvc.startup();
    }

    closingFactory.addServer(new Closeable() {
      @Override
      public void close() throws IOException {
        distributedLockSvc.shutdown();
      }
    });

    injector = Guice.createInjector(
        new VersionModule(), new TimeModule(), new MongoModule(datastore, datastore, distributedLockSvc));
  }

  protected void after() {
    logger.info("Stopping server...");
    closingFactory.stopServers();
  }

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        PersistenceRule.this.before();
        injector.injectMembers(target);
        try {
          statement.evaluate();
        } finally {
          PersistenceRule.this.after();
        }
      }
    };
  }
}
