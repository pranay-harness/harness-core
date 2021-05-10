package io.harness.service.instancehandlerfactory;

import static io.harness.cdng.infra.yaml.InfrastructureKind.KUBERNETES_DIRECT;

import io.harness.dto.infrastructureMapping.InfrastructureMapping;
import io.harness.exception.UnexpectedException;
import io.harness.service.ContainerInstanceHandler;
import io.harness.service.InstanceHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Set;

@Singleton
public class InstanceHandlerFactoryServiceImpl implements InstanceHandlerFactoryService {
  @Inject private ContainerInstanceHandler containerInstanceHandler;

  @Override
  public InstanceHandler getInstanceHandler(InfrastructureMapping infrastructureMapping) {
    return getInstanceHandlerByType(infrastructureMapping.getInfrastructureMappingType());
  }

  @Override
  public InstanceHandler getInstanceHandlerByType(String infrastructureMappingType) {
    switch (infrastructureMappingType) {
      case KUBERNETES_DIRECT:
        return containerInstanceHandler;
      default:
        throw new UnexpectedException(
            "No instance handler defined for infra mapping type: " + infrastructureMappingType);
    }
  }

  @Override
  public Set<InstanceHandler> getAllInstanceHandlers() {
    return null;
  }
}
