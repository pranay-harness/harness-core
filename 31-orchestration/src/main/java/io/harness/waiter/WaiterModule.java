package io.harness.waiter;

import com.google.common.collect.ImmutableSet;
import com.google.inject.TypeLiteral;

import io.harness.govern.DependencyModule;
import io.harness.mongo.MongoQueue;
import io.harness.queue.Queue;
import io.harness.queue.QueueListener;
import io.harness.queue.QueueModule;

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
    bind(new TypeLiteral<Queue<NotifyEvent>>() {}).toInstance(new MongoQueue<>(NotifyEvent.class, 5, true));
    bind(new TypeLiteral<QueueListener<NotifyEvent>>() {}).to(NotifyEventListener.class);
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.<DependencyModule>of(QueueModule.getInstance());
  }
}
