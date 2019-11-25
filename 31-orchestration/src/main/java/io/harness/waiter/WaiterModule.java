package io.harness.waiter;

import static io.harness.queue.Queue.VersionType.VERSIONED;
import static java.time.Duration.ofSeconds;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

import io.harness.config.PublisherConfiguration;
import io.harness.govern.DependencyModule;
import io.harness.mongo.queue.QueueFactory;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;
import io.harness.queue.QueueModule;
import io.harness.queue.QueuePublisher;

import java.util.Set;

public class WaiterModule extends DependencyModule {
  private static WaiterModule instance;

  public static WaiterModule getInstance() {
    if (instance == null) {
      instance = new WaiterModule();
    }
    return instance;
  }

  @Provides
  @Singleton
  QueuePublisher<NotifyEvent> notifyQueuePublisher(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueuePublisher(injector, NotifyEvent.class, VERSIONED, config);
  }

  @Provides
  @Singleton
  QueueConsumer<NotifyEvent> notifyQueueConsumer(Injector injector, PublisherConfiguration config) {
    return QueueFactory.createQueueConsumer(injector, NotifyEvent.class, VERSIONED, ofSeconds(5), config);
  }

  @Override
  protected void configure() {
    bind(new TypeLiteral<QueueListener<NotifyEvent>>() {}).to(NotifyEventListener.class);
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.<DependencyModule>of(QueueModule.getInstance());
  }
}
