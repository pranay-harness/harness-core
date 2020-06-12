package io.harness.cdng;

import io.harness.cdng.artifact.service.ArtifactSourceService;
import io.harness.cdng.artifact.service.impl.ArtifactSourceServiceImpl;
import io.harness.cdng.environment.EnvironmentService;
import io.harness.cdng.environment.EnvironmentServiceImpl;
import io.harness.govern.DependencyModule;
import software.wings.service.impl.artifact.ArtifactServiceImpl;
import software.wings.service.intfc.ArtifactService;

import java.util.Set;

public class NGModule extends DependencyModule {
  private static volatile NGModule instance;

  public static NGModule getInstance() {
    if (instance == null) {
      instance = new NGModule();
    }
    return instance;
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return null;
  }

  @Override
  protected void configure() {
    bind(ArtifactSourceService.class).to(ArtifactSourceServiceImpl.class);
    bind(ArtifactService.class).to(ArtifactServiceImpl.class);
    bind(EnvironmentService.class).to(EnvironmentServiceImpl.class);
  }
}
