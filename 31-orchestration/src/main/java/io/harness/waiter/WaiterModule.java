package io.harness.waiter;

import static io.harness.queue.Queue.VersionType.VERSIONED;
import static java.time.Duration.ofSeconds;

import com.google.common.collect.ImmutableSet;
import com.google.inject.TypeLiteral;

import io.harness.govern.DependencyModule;
import io.harness.mongo.queue.MongoQueueConsumer;
import io.harness.mongo.queue.MongoQueuePublisher;
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

  @Override
  protected void configure() {
    bind(new TypeLiteral<QueuePublisher<NotifyEvent>>() {}).toInstance(new MongoQueuePublisher<>(VERSIONED));
    bind(new TypeLiteral<QueueConsumer<NotifyEvent>>() {})
        .toInstance(new MongoQueueConsumer<>(NotifyEvent.class, VERSIONED, ofSeconds(5)));
    bind(new TypeLiteral<QueueListener<NotifyEvent>>() {}).to(NotifyEventListener.class);
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.<DependencyModule>of(QueueModule.getInstance());
  }
}
