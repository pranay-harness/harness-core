package io.harness.cdng;

import io.harness.NGPipelineCommonsModule;
import io.harness.WalkTreeModule;
import io.harness.cdng.artifact.resources.docker.service.DockerResourceService;
import io.harness.cdng.artifact.resources.docker.service.DockerResourceServiceImpl;
import io.harness.cdng.artifact.service.ArtifactSourceService;
import io.harness.cdng.artifact.service.impl.ArtifactSourceServiceImpl;
import io.harness.cdng.jira.resources.service.JiraResourceService;
import io.harness.cdng.jira.resources.service.JiraResourceServiceImpl;
import io.harness.cdng.pipeline.executions.service.NgPipelineExecutionService;
import io.harness.cdng.pipeline.executions.service.NgPipelineExecutionServiceImpl;
import io.harness.executionplan.ExecutionPlanModule;
import io.harness.ng.core.NGCoreModule;
import io.harness.ngpipeline.pipeline.executions.registries.StageTypeToStageExecutionMapperRegistryModule;
import io.harness.pms.sdk.core.registries.registrar.OrchestrationEventHandlerRegistrar;
import io.harness.registrars.NGStageTypeToStageExecutionSummaryMapperRegistrar;
import io.harness.registrars.OrchestrationExecutionEventHandlerRegistrar;
import io.harness.registrars.StageTypeToStageExecutionMapperRegistrar;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import java.util.concurrent.atomic.AtomicReference;

public class NGModule extends AbstractModule {
  private static final AtomicReference<NGModule> instanceRef = new AtomicReference<>();

  public static NGModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new NGModule());
    }
    return instanceRef.get();
  }

  @Override
  protected void configure() {
    install(NGCoreModule.getInstance());
    install(WalkTreeModule.getInstance());
    install(ExecutionPlanModule.getInstance());
    install(NGPipelineCommonsModule.getInstance());
    install(StageTypeToStageExecutionMapperRegistryModule.getInstance());

    bind(ArtifactSourceService.class).to(ArtifactSourceServiceImpl.class);
    bind(NgPipelineExecutionService.class).to(NgPipelineExecutionServiceImpl.class);
    bind(DockerResourceService.class).to(DockerResourceServiceImpl.class);
    bind(JiraResourceService.class).to(JiraResourceServiceImpl.class);

    MapBinder<String, OrchestrationEventHandlerRegistrar> orchestrationEventHandlerRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, OrchestrationEventHandlerRegistrar.class);
    orchestrationEventHandlerRegistrarMapBinder.addBinding(OrchestrationExecutionEventHandlerRegistrar.class.getName())
        .to(OrchestrationExecutionEventHandlerRegistrar.class);

    MapBinder<String, StageTypeToStageExecutionMapperRegistrar> stageExecutionHelperRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, StageTypeToStageExecutionMapperRegistrar.class);
    stageExecutionHelperRegistrarMapBinder.addBinding(NGStageTypeToStageExecutionSummaryMapperRegistrar.class.getName())
        .to(NGStageTypeToStageExecutionSummaryMapperRegistrar.class);
  }
}
