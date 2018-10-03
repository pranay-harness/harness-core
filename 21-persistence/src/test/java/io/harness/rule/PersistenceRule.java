package io.harness.rule;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.harness.factory.ClosingFactory;
import io.harness.mongo.NoDefaultConstructorMorphiaObjectFactory;
import io.harness.mongo.QueryFactory;
import io.harness.version.VersionModule;
import lombok.Getter;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistenceRule implements MethodRule, MongoRuleMixin {
  private static final Logger logger = LoggerFactory.getLogger(PersistenceRule.class);

  private Injector injector = Guice.createInjector(new VersionModule());

  private ClosingFactory closingFactory = new ClosingFactory();

  @Getter private AdvancedDatastore datastore;

  protected void before() {
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new NoDefaultConstructorMorphiaObjectFactory());
    datastore = (AdvancedDatastore) morphia.createDatastore(fakeMongoClient(0, closingFactory), databaseName());
    datastore.setQueryFactory(new QueryFactory());
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
