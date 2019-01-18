package io.harness.rule;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import io.harness.OrchestrationModule;
import io.harness.factory.ClosingFactory;
import io.harness.mongo.HObjectFactory;
import io.harness.mongo.MongoModule;
import io.harness.mongo.MongoPersistence;
import io.harness.mongo.QueryFactory;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.queue.QueueListenerController;
import io.harness.queue.TimerScheduledExecutorService;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import io.harness.time.TimeModule;
import io.harness.version.VersionModule;
import io.harness.waiter.NotifierScheduledExecutorService;
import io.harness.waiter.NotifyEventListener;
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

public class OrchestrationRule implements MethodRule, InjectorRuleMixin, MongoRuleMixin, DistributedLockRuleMixin {
  private static final Logger logger = LoggerFactory.getLogger(OrchestrationRule.class);

  ClosingFactory closingFactory;
  private AdvancedDatastore datastore;

  public OrchestrationRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) throws Exception {
    ExecutorModule.getInstance().setExecutorService(new CurrentThreadExecutor());

    String databaseName = databaseName();
    MongoInfo mongoInfo = testMongo(annotations, closingFactory);

    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new HObjectFactory());
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

    modules.add(new VersionModule());
    modules.add(new TimeModule());
    modules.add(new MongoModule(datastore, datastore, distributedLockSvc));
    modules.addAll(new OrchestrationModule().cumulativeDependencies());
    return modules;
  }

  @Override
  public void initialize(Injector injector) {
    final QueueListenerController queueListenerController = injector.getInstance(QueueListenerController.class);
    queueListenerController.register(injector.getInstance(NotifyEventListener.class), 1);

    closingFactory.addServer(() -> {
      try {
        queueListenerController.stop();
      } catch (Exception exception) {
        logger.error("", exception);
      }
    });
    closingFactory.addServer(() -> injector.getInstance(TimerScheduledExecutorService.class).shutdown());
    closingFactory.addServer(() -> injector.getInstance(NotifierScheduledExecutorService.class).shutdown());
  }

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return applyInjector(statement, frameworkMethod, target);
  }
}
