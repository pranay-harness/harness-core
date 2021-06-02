package io.harness;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@OwnedBy(CDC)
public class OrchestrationVisualizationModule extends AbstractModule {
  private static OrchestrationVisualizationModule instance;

  public static OrchestrationVisualizationModule getInstance() {
    if (instance == null) {
      instance = new OrchestrationVisualizationModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(GraphGenerationService.class).to(GraphGenerationServiceImpl.class);
    bind(VertexSkipperService.class).to(VertexSkipperServiceImpl.class);
  }

  @Provides
  @Singleton
  @Named("OrchestrationVisualizationExecutorService")
  public ExecutorService orchestrationVisualizationExecutorService() {
    return ThreadPool.create(5, 10, 10, TimeUnit.SECONDS,
        new ThreadFactoryBuilder().setNameFormat("OrchestrationVisualizationExecutorService-%d").build());
  }
}
