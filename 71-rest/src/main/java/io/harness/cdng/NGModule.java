package io.harness.cdng;

import io.harness.cdng.artifact.service.ArtifactService;
import io.harness.cdng.artifact.service.ArtifactSourceService;
import io.harness.cdng.artifact.service.impl.ArtifactServiceImpl;
import io.harness.cdng.artifact.service.impl.ArtifactSourceServiceImpl;
import io.harness.govern.DependencyModule;

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
  }
}
