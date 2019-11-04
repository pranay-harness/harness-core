package io.harness.rule;

import static java.time.Duration.ofSeconds;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import io.harness.factory.ClosingFactory;
import io.harness.govern.ServersModule;
import io.harness.iterator.TestIrregularIterableEntity;
import io.harness.iterator.TestRegularIterableEntity;
import io.harness.module.TestMongoModule;
import io.harness.mongo.HObjectFactory;
import io.harness.mongo.MongoPersistence;
import io.harness.mongo.MongoQueue;
import io.harness.mongo.QueryFactory;
import io.harness.persistence.HPersistence;
import io.harness.queue.Queue;
import io.harness.queue.QueueController;
import io.harness.queue.QueueListenerController;
import io.harness.queue.TestUnversionedQueuableObject;
import io.harness.queue.TestUnversionedQueuableObjectListener;
import io.harness.queue.TestVersionedQueuableObject;
import io.harness.queue.TestVersionedQueuableObjectListener;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import io.harness.time.TimeModule;
import io.harness.version.VersionModule;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PersistenceRule implements MethodRule, InjectorRuleMixin, MongoRuleMixin, DistributedLockRuleMixin {
  ClosingFactory closingFactory;
  @Getter private AdvancedDatastore datastore;

  public PersistenceRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Override
  public void initialize(Injector injector, List<Module> modules) {
    for (Module module : modules) {
      if (module instanceof ServersModule) {
        for (Closeable server : ((ServersModule) module).servers(injector)) {
          closingFactory.addServer(server);
        }
      }
    }

    final QueueListenerController queueListenerController = injector.getInstance(QueueListenerController.class);
    queueListenerController.register(injector.getInstance(TestVersionedQueuableObjectListener.class), 1);
    queueListenerController.register(injector.getInstance(TestUnversionedQueuableObjectListener.class), 1);

    closingFactory.addServer(new Closeable() {
      @Override
      public void close() throws IOException {
        try {
          queueListenerController.stop();
        } catch (Exception exception) {
          throw new IOException(exception);
        }
      }
    });
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) throws Exception {
    ExecutorModule.getInstance().setExecutorService(new CurrentThreadExecutor());

    String databaseName = databaseName();
    MongoInfo mongoInfo = testMongo(annotations, closingFactory);

    final HObjectFactory objectFactory = new HObjectFactory();

    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(objectFactory);
    morphia.map(TestVersionedQueuableObject.class);
    morphia.map(TestRegularIterableEntity.class);
    morphia.map(TestIrregularIterableEntity.class);

    datastore = (AdvancedDatastore) morphia.createDatastore(mongoInfo.getClient(), databaseName);
    datastore.setQueryFactory(new QueryFactory());

    objectFactory.setDatastore(datastore);

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
        bind(new TypeLiteral<Queue<TestVersionedQueuableObject>>() {})
            .toInstance(new MongoQueue<>(TestVersionedQueuableObject.class, ofSeconds(5), true));
        bind(new TypeLiteral<Queue<TestUnversionedQueuableObject>>() {})
            .toInstance(new MongoQueue<>(TestUnversionedQueuableObject.class, ofSeconds(5), true));

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
    modules.addAll(new TestMongoModule(datastore, distributedLockSvc).cumulativeDependencies());

    return modules;
  }

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return applyInjector(statement, frameworkMethod, target);
  }
}
