package io.harness.rule;

import static io.harness.lock.DistributedLockImplementation.MONGO;
import static io.harness.rule.TestUserProvider.testUserProvider;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

import io.harness.factory.ClosingFactory;
import io.harness.factory.ClosingFactoryModule;
import io.harness.govern.ProviderModule;
import io.harness.govern.ServersModule;
import io.harness.lock.DistributedLockImplementation;
import io.harness.lock.PersistentLockModule;
import io.harness.mongo.MongoPersistence;
import io.harness.mongo.queue.MongoQueueConsumer;
import io.harness.mongo.queue.MongoQueuePublisher;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueController;
import io.harness.queue.QueueListenerController;
import io.harness.queue.QueuePublisher;
import io.harness.queue.TestNoTopicQueuableObject;
import io.harness.queue.TestNoTopicQueuableObjectListener;
import io.harness.queue.TestTopicQueuableObject;
import io.harness.queue.TestTopicQueuableObjectListener;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.testlib.module.TestMongoModule;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import io.harness.time.TimeModule;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PersistenceRule implements MethodRule, InjectorRuleMixin, MongoRuleMixin {
  ClosingFactory closingFactory;

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
    queueListenerController.register(injector.getInstance(TestTopicQueuableObjectListener.class), 1);
    queueListenerController.register(injector.getInstance(TestNoTopicQueuableObjectListener.class), 1);

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

    injector.getInstance(HPersistence.class).registerUserProvider(testUserProvider);
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) throws Exception {
    ExecutorModule.getInstance().setExecutorService(new CurrentThreadExecutor());

    List<Module> modules = new ArrayList<>();
    modules.add(new ClosingFactoryModule(closingFactory));
    modules.add(mongoTypeModule(annotations));

    modules.addAll(new TestMongoModule().cumulativeDependencies());

    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      DistributedLockImplementation distributedLockImplementation() {
        return MONGO;
      }
    });
    modules.addAll(new PersistentLockModule().cumulativeDependencies());

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(HPersistence.class).to(MongoPersistence.class);
      }
    });

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        final List<String> topic = asList("topic");
        bind(new TypeLiteral<QueuePublisher<TestTopicQueuableObject>>() {})
            .toInstance(new MongoQueuePublisher<>(TestTopicQueuableObject.class.getSimpleName(), topic));
        final List<List<String>> topicExpression = asList(topic);
        bind(new TypeLiteral<QueueConsumer<TestTopicQueuableObject>>() {})
            .toInstance(new MongoQueueConsumer<>(TestTopicQueuableObject.class, ofSeconds(5), topicExpression));
        bind(new TypeLiteral<QueuePublisher<TestNoTopicQueuableObject>>() {})
            .toInstance(new MongoQueuePublisher<>(TestNoTopicQueuableObject.class.getSimpleName(), topic));
        bind(new TypeLiteral<QueueConsumer<TestNoTopicQueuableObject>>() {})
            .toInstance(new MongoQueueConsumer<>(TestNoTopicQueuableObject.class, ofSeconds(5), topicExpression));

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

    modules.addAll(TimeModule.getInstance().cumulativeDependencies());
    return modules;
  }

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return applyInjector(statement, frameworkMethod, target);
  }
}
