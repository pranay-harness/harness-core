package io.harness.cdng;

import io.harness.cdng.artifact.service.ArtifactSourceService;
import io.harness.cdng.artifact.service.impl.ArtifactSourceServiceImpl;
import io.harness.cdng.common.MiscUtils;
import io.harness.cdng.connectornextgen.impl.ConnectorValidationServiceImpl;
import io.harness.cdng.connectornextgen.impl.KubernetesConnectorServiceImpl;
import io.harness.cdng.connectornextgen.service.ConnectorValidationService;
import io.harness.cdng.connectornextgen.service.KubernetesConnectorService;
import io.harness.cdng.environment.EnvironmentService;
import io.harness.cdng.environment.EnvironmentServiceImpl;
import io.harness.connector.impl.ConnectorServiceImpl;
import io.harness.connector.services.ConnectorService;
import io.harness.govern.DependencyModule;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class NGModule extends DependencyModule {
  private static final AtomicReference<NGModule> instanceRef = new AtomicReference<>();

  public static NGModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new NGModule());
    }
    return instanceRef.get();
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return null;
  }

  @Override
  protected void configure() {
    bind(ArtifactSourceService.class).to(ArtifactSourceServiceImpl.class);
    bind(EnvironmentService.class).to(EnvironmentServiceImpl.class);
    if (!MiscUtils.isNextGenApplication()) {
      // TODO @rk: 12/07/20 : deepak would be removing this, so commenting for now
      bind(ConnectorService.class).to(ConnectorServiceImpl.class);
      bind(ConnectorValidationService.class).to(ConnectorValidationServiceImpl.class);
      bind(KubernetesConnectorService.class).to(KubernetesConnectorServiceImpl.class);
    }
  }
}
