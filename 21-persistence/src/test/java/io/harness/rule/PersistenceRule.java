package io.harness.rule;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import io.harness.factory.ClosingFactory;
import io.harness.module.TestMongoModule;
import io.harness.mongo.HObjectFactory;
import io.harness.mongo.MongoPersistence;
import io.harness.mongo.MongoQueue;
import io.harness.mongo.QueryFactory;
import io.harness.persistence.HPersistence;
import io.harness.queue.Queue;
import io.harness.queue.QueueController;
import io.harness.queue.QueueListenerController;
import io.harness.queue.TestQueuableObject;
import io.harness.queue.TestQueuableObjectListener;
import io.harness.queue.TimerScheduledExecutorService;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
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
import java.util.ArrayList;
import java.util.List;

public class PersistenceRule
    implements MethodRule, InjectorRuleMixin, MongoRuleMixin, DistributedLockRuleMixin, BypassRuleMixin {
  private static final Logger logger = LoggerFactory.getLogger(PersistenceRule.class);

  ClosingFactory closingFactory;
  @Getter private AdvancedDatastore datastore;

  public PersistenceRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Override
  public void initialize(Injector injector) {
    final QueueListenerController queueListenerController = injector.getInstance(QueueListenerController.class);
    queueListenerController.register(injector.getInstance(TestQueuableObjectListener.class), 1);

    closingFactory.addServer(() -> {
      try {
        queueListenerController.stop();
      } catch (Exception exception) {
        logger.error("", exception);
      }
    });
    closingFactory.addServer(() -> injector.getInstance(TimerScheduledExecutorService.class).shutdown());
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) throws Exception {
    ExecutorModule.getInstance().setExecutorService(new CurrentThreadExecutor());

    String databaseName = databaseName();
    MongoInfo mongoInfo = testMongo(annotations, closingFactory);

    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new HObjectFactory());
    morphia.map(TestQueuableObject.class);
    datastore = (AdvancedDatastore) morphia.createDatastore(mongoInfo.getClient(), databaseName);
    datastore.setQueryFactory(new QueryFactory());

    DistributedLockSvc distributedLockSvc = distributedLockSvc(mongoInfo.getClient(), databaseName, closingFactory);

    List<Module> modules = new ArrayList();
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(HPersistence.class).to(MongoPersistence.class);
      }
    });

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(new TypeLiteral<Queue<TestQueuableObject>>() {})
            .toInstance(new MongoQueue<>(TestQueuableObject.class, 5, true));

        bind(QueueController.class).toInstance(new QueueController() {
          @Override
          public boolean isPrimary() {
            return true;
          }

          @Override
          public boolean isNotPrimary() {
            return false;
          }
        });
      }
    });

    modules.add(VersionModule.getInstance());
    modules.addAll(TimeModule.getInstance().cumulativeDependencies());
    modules.add(new TestMongoModule(datastore, datastore, distributedLockSvc));

    return modules;
  }

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    if (bypass(frameworkMethod)) {
      return noopStatement();
    }

    return applyInjector(statement, frameworkMethod, target);
  }
}
