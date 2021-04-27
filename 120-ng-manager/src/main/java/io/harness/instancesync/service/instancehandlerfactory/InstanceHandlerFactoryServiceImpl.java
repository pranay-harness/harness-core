package io.harness.instancesync.service.instancehandlerfactory;

import static io.harness.exception.WingsException.EVERYBODY;
import static io.harness.validation.Validator.notNullCheck;

import io.harness.exception.UnexpectedException;
import io.harness.instancesync.entity.infrastructureMapping.InfrastructureMapping;
import io.harness.instancesync.service.ContainerInstanceHandler;
import io.harness.instancesync.service.InstanceHandler;

import software.wings.beans.InfrastructureMappingType;
import software.wings.utils.Utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Set;

@Singleton
public class InstanceHandlerFactoryServiceImpl implements InstanceHandlerFactoryService {
  @Inject private ContainerInstanceHandler containerInstanceHandler;

  @Override
  public InstanceHandler getInstanceHandler(InfrastructureMapping infraMapping) {
    InfrastructureMappingType infraMappingType =
        Utils.getEnumFromString(InfrastructureMappingType.class, infraMapping.getInfraMappingType());

    notNullCheck("Infra mapping type.", infraMappingType, EVERYBODY);

    switch (infraMappingType) {
      case DIRECT_KUBERNETES:
        return containerInstanceHandler;
      default:
        throw new UnexpectedException("No handler defined for infra mapping type: " + infraMappingType);
    }
  }

  @Override
  public Set<InstanceHandler> getAllInstanceHandlers() {
    return null;
  }
}
