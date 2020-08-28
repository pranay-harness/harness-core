package io.harness.delegate.app;

import com.google.inject.AbstractModule;

import io.harness.cdng.artifact.delegate.DockerArtifactServiceImpl;
import io.harness.cdng.artifact.delegate.resource.DockerRegistryService;
import io.harness.cdng.artifact.delegate.resource.DockerRegistryServiceImpl;

public class NGDelegateModule extends AbstractModule {
  private static volatile NGDelegateModule instance;

  public static NGDelegateModule getInstance() {
    if (instance == null) {
      instance = new NGDelegateModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(DockerRegistryService.class).to(DockerRegistryServiceImpl.class);
    bind(DockerArtifactServiceImpl.class);
  }
}
