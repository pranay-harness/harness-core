package io.harness.rule;

import static io.harness.waiter.TestNotifyEventListener.TEST_PUBLISHER;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

import io.harness.OrchestrationModule;
import io.harness.config.PublisherConfiguration;
import io.harness.factory.ClosingFactory;
import io.harness.govern.ProviderModule;
import io.harness.govern.ServersModule;
import io.harness.module.TestMongoModule;
import io.harness.mongo.HObjectFactory;
import io.harness.mongo.MongoPersistence;
import io.harness.mongo.QueryFactory;
import io.harness.mongo.queue.QueueFactory;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueController;
import io.harness.queue.QueueListenerController;
import io.harness.queue.QueuePublisher;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import io.harness.time.TimeModule;
import io.harness.version.VersionInfoManager;
import io.harness.version.VersionModule;
import io.harness.waiter.NotifyEvent;
import io.harness.waiter.NotifyQueuePublisherRegister;
import io.harness.waiter.TestNotifyEventListener;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class OrchestrationRule implements MethodRule, InjectorRuleMixin, MongoRuleMixin {
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
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      QueueConsumer<NotifyEvent> notifyQueueConsumer(
          Injector injector, VersionInfoManager versionInfoManager, PublisherConfiguration config) {
        return QueueFactory.createQueueConsumer(injector, NotifyEvent.class, ofSeconds(5),
            asList(asList(versionInfoManager.getVersionInfo().getVersion()), asList(TEST_PUBLISHER)), config);
      }
    });

    modules.addAll(TimeModule.getInstance().cumulativeDependencies());
    modules.addAll(new TestMongoModule(datastore).cumulativeDependencies());
    modules.addAll(new OrchestrationModule().cumulativeDependencies());
    return modules;
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
    queueListenerController.register(injector.getInstance(TestNotifyEventListener.class), 1);

    final QueuePublisher<NotifyEvent> publisher =
        injector.getInstance(Key.get(new TypeLiteral<QueuePublisher<NotifyEvent>>() {}));
    final NotifyQueuePublisherRegister notifyQueuePublisherRegister =
        injector.getInstance(NotifyQueuePublisherRegister.class);
    notifyQueuePublisherRegister.register(TEST_PUBLISHER, payload -> publisher.send(asList(TEST_PUBLISHER), payload));
  }

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return applyInjector(statement, frameworkMethod, target);
  }
}
