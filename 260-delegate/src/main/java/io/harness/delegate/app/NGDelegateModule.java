package io.harness.delegate.app;

import io.harness.artifacts.docker.service.DockerRegistryService;
import io.harness.artifacts.docker.service.DockerRegistryServiceImpl;
import io.harness.delegate.task.artifacts.docker.DockerArtifactTaskHandler;
import io.harness.http.HttpService;
import io.harness.http.HttpServiceImpl;

import com.google.inject.AbstractModule;

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
    bind(HttpService.class).to(HttpServiceImpl.class);
    bind(DockerArtifactTaskHandler.class);
  }
}
