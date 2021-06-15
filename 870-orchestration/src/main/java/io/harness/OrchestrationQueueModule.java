package io.harness;

import static java.util.Collections.emptyList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.config.PublisherConfiguration;
import io.harness.mongo.queue.QueueFactory;
import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.queue.QueuePublisher;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
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
  protected void configure() {}

  @Provides
  @Singleton
  QueuePublisher<NodeExecutionEvent> executionEventQueuePublisher(
      Injector injector, PublisherConfiguration config, MongoTemplate mongoTemplate) {
    return QueueFactory.createNgQueuePublisher(injector, NodeExecutionEvent.class, emptyList(), config, mongoTemplate);
  }
}
