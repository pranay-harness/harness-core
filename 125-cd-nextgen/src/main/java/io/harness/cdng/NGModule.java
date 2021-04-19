package io.harness.cdng;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.NGPipelineCommonsModule;
import io.harness.OrchestrationModuleConfig;
import io.harness.WalkTreeModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.resources.docker.service.DockerResourceService;
import io.harness.cdng.artifact.resources.docker.service.DockerResourceServiceImpl;
import io.harness.cdng.artifact.resources.ecr.service.EcrResourceService;
import io.harness.cdng.artifact.resources.ecr.service.EcrResourceServiceImpl;
import io.harness.cdng.artifact.resources.gcr.service.GcrResourceService;
import io.harness.cdng.artifact.resources.gcr.service.GcrResourceServiceImpl;
import io.harness.cdng.artifact.service.ArtifactSourceService;
import io.harness.cdng.artifact.service.impl.ArtifactSourceServiceImpl;
import io.harness.cdng.jira.resources.service.JiraResourceService;
import io.harness.cdng.jira.resources.service.JiraResourceServiceImpl;
import io.harness.cdng.k8s.resources.gcp.service.GcpResourceService;
import io.harness.cdng.k8s.resources.gcp.service.impl.GcpResourceServiceImpl;
import io.harness.cdng.pipeline.executions.service.NgPipelineExecutionService;
import io.harness.cdng.pipeline.executions.service.NgPipelineExecutionServiceImpl;
import io.harness.cdng.yaml.CdYamlSchemaService;
import io.harness.cdng.yaml.CdYamlSchemaServiceImpl;
import io.harness.configManager.ConfigurationController;
import io.harness.executionplan.ExecutionPlanModule;
import io.harness.ng.core.NGCoreModule;
import io.harness.ngpipeline.pipeline.executions.registries.StageTypeToStageExecutionMapperRegistryModule;
import io.harness.queue.QueueController;
import io.harness.registrars.NGStageTypeToStageExecutionSummaryMapperRegistrar;
import io.harness.registrars.StageTypeToStageExecutionMapperRegistrar;
import io.harness.version.VersionInfoManager;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.io.IOUtils;

@OwnedBy(CDP)
public class NGModule extends AbstractModule {
  private static final AtomicReference<NGModule> instanceRef = new AtomicReference<>();
  private final OrchestrationModuleConfig config;

  public static NGModule getInstance(OrchestrationModuleConfig config) {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new NGModule(config));
    }
    return instanceRef.get();
  }

  public NGModule(OrchestrationModuleConfig config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    try {
      VersionInfoManager versionInfoManager = new VersionInfoManager(
          IOUtils.toString(getClass().getClassLoader().getResourceAsStream("main/resources-filtered/versionInfo.yaml"),
              StandardCharsets.UTF_8));
      bind(VersionInfoManager.class).toInstance(versionInfoManager);
    } catch (IOException e) {
      throw new IllegalStateException("Could not load versionInfo.yaml", e);
    }

    install(NGCoreModule.getInstance());
    install(WalkTreeModule.getInstance());
    install(ExecutionPlanModule.getInstance());
    install(NGPipelineCommonsModule.getInstance(config));
    install(StageTypeToStageExecutionMapperRegistryModule.getInstance());

    bind(QueueController.class).toInstance(new ConfigurationController());
    bind(ArtifactSourceService.class).to(ArtifactSourceServiceImpl.class);
    bind(NgPipelineExecutionService.class).to(NgPipelineExecutionServiceImpl.class);
    bind(DockerResourceService.class).to(DockerResourceServiceImpl.class);
    bind(GcrResourceService.class).to(GcrResourceServiceImpl.class);
    bind(EcrResourceService.class).to(EcrResourceServiceImpl.class);
    bind(JiraResourceService.class).to(JiraResourceServiceImpl.class);
    bind(CdYamlSchemaService.class).to(CdYamlSchemaServiceImpl.class);
    bind(GcpResourceService.class).to(GcpResourceServiceImpl.class);

    MapBinder<String, StageTypeToStageExecutionMapperRegistrar> stageExecutionHelperRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, StageTypeToStageExecutionMapperRegistrar.class);
    stageExecutionHelperRegistrarMapBinder.addBinding(NGStageTypeToStageExecutionSummaryMapperRegistrar.class.getName())
        .to(NGStageTypeToStageExecutionSummaryMapperRegistrar.class);
  }
}
