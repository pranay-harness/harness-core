package io.harness;

import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

import io.harness.delay.DelayEventListener;
import io.harness.engine.expressions.AmbianceExpressionEvaluatorProvider;
import io.harness.factory.ClosingFactory;
import io.harness.factory.ClosingFactoryModule;
import io.harness.govern.ProviderModule;
import io.harness.govern.ServersModule;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.queue.QueueListenerController;
import io.harness.queue.QueuePublisher;
import io.harness.rule.InjectorRuleMixin;
import io.harness.serializer.KryoModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.OrchestrationRegistrars;
import io.harness.serializer.kryo.OrchestrationTestKryoRegistrar;
import io.harness.serializer.kryo.TestPersistenceKryoRegistrar;
import io.harness.serializer.morphia.TestPersistenceMorphiaRegistrar;
import io.harness.serializer.spring.OrchestrationTestSpringAliasRegistrar;
import io.harness.spring.AliasRegistrar;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import io.harness.time.TimeModule;
import io.harness.version.VersionModule;
import io.harness.waiter.NotifierScheduledExecutorService;
import io.harness.waiter.NotifyEvent;
import io.harness.waiter.NotifyQueuePublisherRegister;
import io.harness.waiter.NotifyResponseCleaner;
import io.harness.waiter.OrchestrationNotifyEventListener;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class OrchestrationRule implements MethodRule, InjectorRuleMixin, MongoRuleMixin {
  ClosingFactory closingFactory;

  public OrchestrationRule(ClosingFactory closingFactory) {
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
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
            .addAll(OrchestrationRegistrars.kryoRegistrars)
            .add(TestPersistenceKryoRegistrar.class)
            .add(OrchestrationTestKryoRegistrar.class)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(OrchestrationRegistrars.morphiaRegistrars)
            .add(TestPersistenceMorphiaRegistrar.class)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends AliasRegistrar>> aliasRegistrars() {
        return ImmutableSet.<Class<? extends AliasRegistrar>>builder()
            .addAll(OrchestrationRegistrars.aliasRegistrars)
            .add(OrchestrationTestSpringAliasRegistrar.class)
            .build();
      }
    });

    modules.add(mongoTypeModule(annotations));

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
    modules.add(TimeModule.getInstance());
    modules.add(new OrchestrationPersistenceTestModule());
    modules.add(
        OrchestrationModule.getInstance(OrchestrationModuleConfig.builder()
                                            .expressionEvaluatorProvider(new AmbianceExpressionEvaluatorProvider())
                                            .build()));
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
    queueListenerController.register(injector.getInstance(OrchestrationNotifyEventListener.class), 1);
    queueListenerController.register(injector.getInstance(DelayEventListener.class), 1);

    closingFactory.addServer(new Closeable() {
      @SneakyThrows
      @Override
      public void close() {
        queueListenerController.stop();
      }
    });

    final QueuePublisher<NotifyEvent> publisher =
        injector.getInstance(Key.get(new TypeLiteral<QueuePublisher<NotifyEvent>>() {}));
    final NotifyQueuePublisherRegister notifyQueuePublisherRegister =
        injector.getInstance(NotifyQueuePublisherRegister.class);
    notifyQueuePublisherRegister.register(
        ORCHESTRATION, payload -> publisher.send(Collections.singletonList(ORCHESTRATION), payload));

    NotifierScheduledExecutorService notifierScheduledExecutorService =
        injector.getInstance(NotifierScheduledExecutorService.class);
    notifierScheduledExecutorService.scheduleWithFixedDelay(
        injector.getInstance(NotifyResponseCleaner.class), 0L, 1000L, TimeUnit.MILLISECONDS);
    closingFactory.addServer(new Closeable() {
      @Override
      public void close() throws IOException {
        notifierScheduledExecutorService.shutdown();
      }
    });
  }

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return applyInjector(statement, frameworkMethod, target);
  }
}