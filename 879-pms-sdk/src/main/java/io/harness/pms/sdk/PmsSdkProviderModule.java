package io.harness.pms.sdk;

import static java.time.Duration.ofSeconds;

import io.harness.config.PublisherConfiguration;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.queue.QueueFactory;
import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.pms.sdk.core.pipeline.filters.FilterCreationResponseMerger;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;
import io.harness.pms.sdk.registries.StepRegistry;
import io.harness.queue.QueueConsumer;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.util.Collections;
import java.util.Set;
import org.springframework.data.mongodb.core.MongoTemplate;

class PmsSdkProviderModule extends AbstractModule {
  private final PmsSdkConfiguration config;
  private final String serviceName;

  private static PmsSdkProviderModule instance;

  public static PmsSdkProviderModule getInstance(PmsSdkConfiguration config, String serviceName) {
    if (instance == null) {
      instance = new PmsSdkProviderModule(config, serviceName);
    }
    return instance;
  }

  private PmsSdkProviderModule(PmsSdkConfiguration config, String serviceName) {
    this.config = config;
    this.serviceName = serviceName;
  }

  @Provides
  @Singleton
  public QueueConsumer<NodeExecutionEvent> nodeExecutionEventQueueConsumer(
      Injector injector, PublisherConfiguration config) {
    MongoTemplate sdkTemplate = injector.getInstance(Key.get(MongoTemplate.class, Names.named("pmsSdkMongoTemplate")));
    return QueueFactory.createNgQueueConsumer(injector, NodeExecutionEvent.class, ofSeconds(5),
        Collections.singletonList(Collections.singletonList(serviceName)), config, sdkTemplate);
  }

  @Provides
  @Singleton
  public PipelineServiceInfoProvider pipelineServiceInfoProvider() {
    return config.getPipelineServiceInfoProvider();
  }

  @Provides
  @Singleton
  public FilterCreationResponseMerger filterCreationResponseMerger() {
    return config.getFilterCreationResponseMerger();
  }

  @Provides
  @Singleton
  public ServiceManager serviceManager(Set<Service> services) {
    return new ServiceManager(services);
  }

  @Provides
  @Singleton
  @Named("pmsSdkMongoConfig")
  public MongoConfig mongoConfig() {
    return config.getMongoConfig();
  }
}
