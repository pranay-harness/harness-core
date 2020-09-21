package io.harness.cdng;

import com.google.inject.AbstractModule;

import io.harness.NGPipelineCommonsModule;
import io.harness.cdng.artifact.resources.docker.service.DockerResourceService;
import io.harness.cdng.artifact.resources.docker.service.DockerResourceServiceImpl;
import io.harness.cdng.artifact.service.ArtifactSourceService;
import io.harness.cdng.artifact.service.impl.ArtifactSourceServiceImpl;
import io.harness.cdng.inputset.services.CDInputSetEntityService;
import io.harness.cdng.inputset.services.impl.CDInputSetEntityServiceImpl;
import io.harness.cdng.pipeline.service.NgPipelineExecutionService;
import io.harness.cdng.pipeline.service.NgPipelineExecutionServiceImpl;
import io.harness.cdng.pipeline.service.PipelineService;
import io.harness.cdng.pipeline.service.PipelineServiceImpl;
import io.harness.ng.core.NGCoreModule;

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
    install(NGPipelineCommonsModule.getInstance());

    bind(ArtifactSourceService.class).to(ArtifactSourceServiceImpl.class);
    bind(PipelineService.class).to(PipelineServiceImpl.class);
    bind(NgPipelineExecutionService.class).to(NgPipelineExecutionServiceImpl.class);
    bind(DockerResourceService.class).to(DockerResourceServiceImpl.class);
    bind(CDInputSetEntityService.class).to(CDInputSetEntityServiceImpl.class);
  }
}
