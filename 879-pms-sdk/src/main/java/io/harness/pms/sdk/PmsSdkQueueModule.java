package io.harness.pms.sdk;

import static java.time.Duration.ofSeconds;

import io.harness.config.PublisherConfiguration;
import io.harness.mongo.queue.QueueFactory;
import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.pms.sdk.core.execution.NodeExecutionEventListener;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueController;
import io.harness.queue.QueueListener;
import io.harness.version.VersionInfoManager;

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
    bind(QueueController.class).toInstance(new PmsSdkQueueController());
    bind(new TypeLiteral<QueueListener<NodeExecutionEvent>>() {}).to(NodeExecutionEventListener.class);
  }

  @Provides
  @Singleton
  public QueueConsumer<NodeExecutionEvent> nodeExecutionEventQueueConsumer(
      Injector injector, PublisherConfiguration publisherConfiguration, VersionInfoManager versionInfoManager) {
    MongoTemplate sdkTemplate = injector.getInstance(Key.get(MongoTemplate.class, Names.named("pmsSdkMongoTemplate")));
    List<List<String>> topicExpressions = ImmutableList.of(
        ImmutableList.of(versionInfoManager.getVersionInfo().getVersion()), ImmutableList.of(config.getServiceName()));
    return QueueFactory.createNgQueueConsumer(
        injector, NodeExecutionEvent.class, ofSeconds(5), topicExpressions, publisherConfiguration, sdkTemplate);
  }

  private static class PmsSdkQueueController implements QueueController {
    @Override
    public boolean isPrimary() {
      return true;
    }

    @Override
    public boolean isNotPrimary() {
      return false;
    }
  }
}
