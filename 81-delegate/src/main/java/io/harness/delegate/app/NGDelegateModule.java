package io.harness.delegate.app;

import io.harness.cdng.artifact.delegate.DockerArtifactServiceImpl;
import io.harness.cdng.artifact.delegate.resource.DockerRegistryService;
import io.harness.cdng.artifact.delegate.resource.DockerRegistryServiceImpl;
import io.harness.govern.DependencyModule;

import java.util.Set;

public class NGDelegateModule extends DependencyModule {
  private static volatile NGDelegateModule instance;

  public static NGDelegateModule getInstance() {
    if (instance == null) {
      instance = new NGDelegateModule();
    }
    return instance;
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return null;
  }

  @Override
  protected void configure() {
    bind(DockerRegistryService.class).to(DockerRegistryServiceImpl.class);
    bind(DockerArtifactServiceImpl.class);
  }
}
