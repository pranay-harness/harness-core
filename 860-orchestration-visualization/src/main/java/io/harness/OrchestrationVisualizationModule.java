package io.harness;

import static io.harness.AuthorizationServiceHeader.PIPELINE_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.eventsframework.EventsFrameworkConstants.ORCHESTRATION_LOG;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.impl.noop.NoOpConsumer;
import io.harness.eventsframework.impl.redis.RedisConsumer;
import io.harness.graph.stepDetail.PmsGraphStepDetailsServiceImpl;
import io.harness.graph.stepDetail.service.PmsGraphStepDetailsService;
import io.harness.redis.RedisConfig;
import io.harness.service.GraphGenerationService;
import io.harness.service.impl.GraphGenerationServiceImpl;
import io.harness.skip.service.VertexSkipperService;
import io.harness.skip.service.impl.VertexSkipperServiceImpl;
import io.harness.threading.ThreadPool;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@OwnedBy(CDC)
public class OrchestrationVisualizationModule extends AbstractModule {
  private static OrchestrationVisualizationModule instance;
  private final EventsFrameworkConfiguration eventsFrameworkConfiguration;

  public static OrchestrationVisualizationModule getInstance(
      EventsFrameworkConfiguration eventsFrameworkConfiguration) {
    if (instance == null) {
      instance = new OrchestrationVisualizationModule(eventsFrameworkConfiguration);
    }
    return instance;
  }

  OrchestrationVisualizationModule(EventsFrameworkConfiguration eventsFrameworkConfiguration) {
    this.eventsFrameworkConfiguration = eventsFrameworkConfiguration;
  }

  @Override
  protected void configure() {
    bind(GraphGenerationService.class).to(GraphGenerationServiceImpl.class);
    bind(PmsGraphStepDetailsService.class).to(PmsGraphStepDetailsServiceImpl.class);
    bind(VertexSkipperService.class).to(VertexSkipperServiceImpl.class);
    RedisConfig redisConfig = this.eventsFrameworkConfiguration.getRedisConfig();
    if (redisConfig.getRedisUrl().equals("dummyRedisUrl")) {
      bind(Consumer.class)
          .annotatedWith(Names.named(ORCHESTRATION_LOG))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
    } else {
      bind(Consumer.class)
          .annotatedWith(Names.named(ORCHESTRATION_LOG))
          .toInstance(RedisConsumer.of(ORCHESTRATION_LOG, PIPELINE_SERVICE.getServiceId(), redisConfig,
              EventsFrameworkConstants.ORCHESTRATION_LOG_MAX_PROCESSING_TIME,
              EventsFrameworkConstants.ORCHESTRATION_LOG_READ_BATCH_SIZE));
    }
  }

  @Provides
  @Singleton
  @Named("OrchestrationVisualizationExecutorService")
  public ExecutorService orchestrationVisualizationExecutorService() {
    return ThreadPool.create(4, 8, 10, TimeUnit.SECONDS,
        new ThreadFactoryBuilder().setNameFormat("OrchestrationVisualizationExecutorService-%d").build());
  }
}
