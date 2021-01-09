package io.harness.pms.sdk;

import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import io.harness.config.PublisherConfiguration;
import io.harness.mongo.queue.QueueFactory;
import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.pms.sdk.PmsSdkConfiguration.DeployMode;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.execution.SdkOrchestrationEventListener;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;

import com.google.common.collect.ImmutableList;
import com.google.inject.*;
import com.google.inject.name.Names;
import java.util.List;
import org.springframework.data.mongodb.core.MongoTemplate;

public class PmsSdkQueueModule extends AbstractModule {
  private final PmsSdkConfiguration config;

  private static PmsSdkQueueModule instance;

  public static PmsSdkQueueModule getInstance(PmsSdkConfiguration config) {
    if (instance == null) {
      instance = new PmsSdkQueueModule(config);
    }
    return instance;
  }

  private PmsSdkQueueModule(PmsSdkConfiguration config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    if (config.getDeploymentMode() == DeployMode.REMOTE) {
      bind(new TypeLiteral<QueueListener<OrchestrationEvent>>() {}).to(SdkOrchestrationEventListener.class);
    }
  }

  @Provides
  @Singleton
  public QueueConsumer<NodeExecutionEvent> nodeExecutionEventQueueConsumer(
      Injector injector, PublisherConfiguration publisherConfiguration) {
    if (this.config.getDeploymentMode().isNonLocal()) {
      MongoTemplate sdkTemplate = getMongoTemplate(injector);
      List<List<String>> topicExpressions = singletonList(singletonList(config.getServiceName()));
      return QueueFactory.createNgQueueConsumer(
          injector, NodeExecutionEvent.class, ofSeconds(5), topicExpressions, publisherConfiguration, sdkTemplate);
    }
    MongoTemplate mongoTemplate = injector.getInstance(MongoTemplate.class);
    List<List<String>> topicExpressions = ImmutableList.of(singletonList("_pms_"));
    return QueueFactory.createNgQueueConsumer(
        injector, NodeExecutionEvent.class, ofSeconds(3), topicExpressions, publisherConfiguration, mongoTemplate);
  }

  @Provides
  @Singleton
  public QueueConsumer<OrchestrationEvent> orchestrationEventQueueConsumer(
      Injector injector, PublisherConfiguration publisherConfiguration) {
    if (this.config.getDeploymentMode().isNonLocal()) {
      MongoTemplate sdkTemplate = getMongoTemplate(injector);
      List<List<String>> topicExpressions = singletonList(singletonList(config.getServiceName()));
      return QueueFactory.createNgQueueConsumer(
          injector, OrchestrationEvent.class, ofSeconds(5), topicExpressions, publisherConfiguration, sdkTemplate);
    }
    MongoTemplate mongoTemplate = injector.getInstance(MongoTemplate.class);
    return QueueFactory.createNgQueueConsumer(
        injector, OrchestrationEvent.class, ofSeconds(5), emptyList(), publisherConfiguration, mongoTemplate);
  }

  private MongoTemplate getMongoTemplate(Injector injector) {
    if (config.getDeploymentMode() == DeployMode.REMOTE_IN_PROCESS) {
      return injector.getInstance(MongoTemplate.class);
    } else {
      return injector.getInstance(Key.get(MongoTemplate.class, Names.named("pmsSdkMongoTemplate")));
    }
  }
}
