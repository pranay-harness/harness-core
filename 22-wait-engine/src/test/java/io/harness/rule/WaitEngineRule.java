package io.harness.rule;

import static io.harness.waiter.TestNotifyEventListener.TEST_PUBLISHER;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

import io.harness.WaitEngineRegistrars;
import io.harness.config.PublisherConfiguration;
import io.harness.factory.ClosingFactory;
import io.harness.factory.ClosingFactoryModule;
import io.harness.govern.ProviderModule;
import io.harness.govern.ServersModule;
import io.harness.mongo.MongoPersistence;
import io.harness.mongo.queue.QueueFactory;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueController;
import io.harness.queue.QueueListenerController;
import io.harness.queue.QueuePublisher;
import io.harness.serializer.KryoModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.kryo.TestPersistenceKryoRegistrar;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.testlib.module.TestMongoModule;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import io.harness.version.VersionInfoManager;
import io.harness.version.VersionModule;
import io.harness.waiter.NotifierScheduledExecutorService;
import io.harness.waiter.NotifyEvent;
import io.harness.waiter.NotifyQueuePublisherRegister;
import io.harness.waiter.NotifyResponseCleaner;
import io.harness.waiter.TestNotifyEventListener;
import io.harness.waiter.WaiterModule;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WaitEngineRule implements MethodRule, InjectorRuleMixin, MongoRuleMixin {
  ClosingFactory closingFactory;

  public WaitEngineRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) throws Exception {
    ExecutorModule.getInstance().setExecutorService(new CurrentThreadExecutor());

    List<Module> modules = new ArrayList<>();
    modules.add(new ClosingFactoryModule(closingFactory));
    modules.add(KryoModule.getInstance());
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> registrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
            .addAll(WaitEngineRegistrars.kryoRegistrars)
            .add(TestPersistenceKryoRegistrar.class)
            .build();
      }
    });

    modules.add(mongoTypeModule(annotations));

    modules.add(WaiterModule.getInstance());

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

    modules.add(TestMongoModule.getInstance());
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
    notifyQueuePublisherRegister.register(
        TEST_PUBLISHER, payload -> publisher.send(Collections.singletonList(TEST_PUBLISHER), payload));

    injector.getInstance(NotifierScheduledExecutorService.class)
        .scheduleWithFixedDelay(injector.getInstance(NotifyResponseCleaner.class), 0L, 1000L, TimeUnit.MILLISECONDS);
  }

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return applyInjector(statement, frameworkMethod, target);
  }
}
