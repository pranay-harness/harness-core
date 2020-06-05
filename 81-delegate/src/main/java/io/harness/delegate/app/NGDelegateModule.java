package io.harness.delegate.app;

import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;

import io.harness.cdng.artifact.bean.connector.ConnectorConfig;
import io.harness.cdng.artifact.bean.connector.DockerhubConnectorConfig;
import io.harness.cdng.artifact.delegate.DelegateArtifactService;
import io.harness.cdng.artifact.delegate.DockerArtifactService;
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
    bind(DockerArtifactService.class).to(DockerArtifactServiceImpl.class);
    bind(DockerRegistryService.class).to(DockerRegistryServiceImpl.class);

    MapBinder<Class<? extends ConnectorConfig>, Class<? extends DelegateArtifactService>> artifactServiceMapBinder =
        MapBinder.newMapBinder(binder(), new TypeLiteral<Class<? extends ConnectorConfig>>() {},
            new TypeLiteral<Class<? extends DelegateArtifactService>>() {});
    artifactServiceMapBinder.addBinding(DockerhubConnectorConfig.class).toInstance(DockerArtifactService.class);
  }
}
