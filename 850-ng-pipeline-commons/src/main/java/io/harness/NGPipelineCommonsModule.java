package io.harness;

import io.harness.ngpipeline.pipeline.service.NGPipelineService;
import io.harness.ngpipeline.pipeline.service.NGPipelineServiceImpl;
import io.harness.threading.ThreadPool;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class NGPipelineCommonsModule extends AbstractModule {
  private static final AtomicReference<NGPipelineCommonsModule> instanceRef = new AtomicReference<>();

  public static NGPipelineCommonsModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new NGPipelineCommonsModule());
    }
    return instanceRef.get();
  }

  @Override
  protected void configure() {
    bind(NGPipelineService.class).to(NGPipelineServiceImpl.class);
    bind(ExecutorService.class)
        .annotatedWith(Names.named("NgPipelineCommonsExecutor"))
        .toInstance(ThreadPool.create(1, 1, 5, TimeUnit.SECONDS,
            new ThreadFactoryBuilder()
                .setNameFormat("default-ng-pipeline-commons-executor-%d")
                .setPriority(Thread.MIN_PRIORITY)
                .build()));
  }
}
