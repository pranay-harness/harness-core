package io.harness;

import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.config.PublisherConfiguration;
import io.harness.execution.SdkResponseEventListener;
import io.harness.mongo.queue.QueueFactory;
import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.pms.execution.SdkResponseEvent;
import io.harness.pms.interrupts.InterruptEvent;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;
import io.harness.queue.QueuePublisher;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(HarnessTeam.PIPELINE)
public class OrchestrationQueueModule extends AbstractModule {
  private static OrchestrationQueueModule instance;

  public static synchronized OrchestrationQueueModule getInstance() {
    if (instance == null) {
      instance = new OrchestrationQueueModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(new TypeLiteral<QueueListener<SdkResponseEvent>>() {}).to(SdkResponseEventListener.class);
  }

  @Provides
  @Singleton
  QueueConsumer<SdkResponseEvent> sdkResponseEventQueueConsumer(
      Injector injector, PublisherConfiguration config, MongoTemplate mongoTemplate) {
    return QueueFactory.createNgQueueConsumer(
        injector, SdkResponseEvent.class, ofSeconds(5), emptyList(), config, mongoTemplate);
  }

  @Provides
  @Singleton
  QueuePublisher<OrchestrationEvent> orchestrationEventQueuePublisher(
      Injector injector, PublisherConfiguration config, MongoTemplate mongoTemplate) {
    return QueueFactory.createNgQueuePublisher(injector, OrchestrationEvent.class, emptyList(), config, mongoTemplate);
  }

  @Provides
  @Singleton
  QueuePublisher<NodeExecutionEvent> executionEventQueuePublisher(
      Injector injector, PublisherConfiguration config, MongoTemplate mongoTemplate) {
    return QueueFactory.createNgQueuePublisher(injector, NodeExecutionEvent.class, emptyList(), config, mongoTemplate);
  }

  @Provides
  @Singleton
  QueuePublisher<InterruptEvent> interruptEventQueuePublisher(
      Injector injector, PublisherConfiguration config, MongoTemplate mongoTemplate) {
    return QueueFactory.createNgQueuePublisher(injector, InterruptEvent.class, emptyList(), config, mongoTemplate);
  }
}
