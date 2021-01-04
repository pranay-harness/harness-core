package io.harness;

import io.harness.ngpipeline.inputset.services.InputSetEntityService;
import io.harness.ngpipeline.inputset.services.impl.InputSetEntityServiceImpl;
import io.harness.ngpipeline.pipeline.service.NGPipelineService;
import io.harness.ngpipeline.pipeline.service.NGPipelineServiceImpl;
import io.harness.registrars.NGPipelineVisitorFieldRegistrar;
import io.harness.threading.ThreadPool;
import io.harness.walktree.registries.registrars.VisitableFieldRegistrar;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class NGPipelineCommonsModule extends AbstractModule {
  private static final AtomicReference<NGPipelineCommonsModule> instanceRef = new AtomicReference<>();
  private final OrchestrationModuleConfig config;

  public static NGPipelineCommonsModule getInstance(OrchestrationModuleConfig config) {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new NGPipelineCommonsModule(config));
    }
    return instanceRef.get();
  }

  public NGPipelineCommonsModule(OrchestrationModuleConfig config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    install(OrchestrationModule.getInstance(config));

    MapBinder<String, VisitableFieldRegistrar> visitableFieldRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, VisitableFieldRegistrar.class);
    visitableFieldRegistrarMapBinder.addBinding(NGPipelineVisitorFieldRegistrar.class.getName())
        .to(NGPipelineVisitorFieldRegistrar.class);

    bind(NGPipelineService.class).to(NGPipelineServiceImpl.class);
    bind(InputSetEntityService.class).to(InputSetEntityServiceImpl.class);
    bind(ExecutorService.class)
        .annotatedWith(Names.named("NgPipelineCommonsExecutor"))
        .toInstance(ThreadPool.create(1, 1, 5, TimeUnit.SECONDS,
            new ThreadFactoryBuilder()
                .setNameFormat("default-ng-pipeline-commons-executor-%d")
                .setPriority(Thread.MIN_PRIORITY)
                .build()));
  }
}
