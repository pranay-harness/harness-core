package io.harness.pms.sdk.core;

import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.config.PublisherConfiguration;
import io.harness.mongo.queue.QueueFactory;
import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.pms.sdk.core.execution.events.node.NodeExecutionEventListener;
import io.harness.pms.utils.PmsConstants;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;
import io.harness.queue.QueueListenerController;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import java.util.List;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsSdkQueueModule extends AbstractModule {
  private final PmsSdkCoreConfig config;

  private static PmsSdkQueueModule instance;

  public static PmsSdkQueueModule getInstance(PmsSdkCoreConfig config) {
    if (instance == null) {
      instance = new PmsSdkQueueModule(config);
    }
    return instance;
  }

  private PmsSdkQueueModule(PmsSdkCoreConfig config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    bind(new TypeLiteral<QueueListener<NodeExecutionEvent>>() {}).to(NodeExecutionEventListener.class);
    requireBinding(QueueListenerController.class);
  }

  @Provides
  @Singleton
  public QueueConsumer<NodeExecutionEvent> nodeExecutionEventQueueConsumer(
      Injector injector, PublisherConfiguration publisherConfiguration) {
    if (this.config.getSdkDeployMode().isNonLocal()) {
      MongoTemplate sdkTemplate = getMongoTemplate(injector);
      List<List<String>> topicExpressions = singletonList(singletonList(config.getServiceName()));
      return QueueFactory.createNgQueueConsumer(
          injector, NodeExecutionEvent.class, ofSeconds(5), topicExpressions, publisherConfiguration, sdkTemplate);
    }
    MongoTemplate mongoTemplate = injector.getInstance(MongoTemplate.class);
    List<List<String>> topicExpressions = ImmutableList.of(singletonList(PmsConstants.INTERNAL_SERVICE_NAME));
    return QueueFactory.createNgQueueConsumer(
        injector, NodeExecutionEvent.class, ofSeconds(3), topicExpressions, publisherConfiguration, mongoTemplate);
  }

  private MongoTemplate getMongoTemplate(Injector injector) {
    if (config.getSdkDeployMode() == SdkDeployMode.REMOTE_IN_PROCESS) {
      return injector.getInstance(MongoTemplate.class);
    } else {
      return injector.getInstance(Key.get(MongoTemplate.class, Names.named("pmsSdkMongoTemplate")));
    }
  }
}
