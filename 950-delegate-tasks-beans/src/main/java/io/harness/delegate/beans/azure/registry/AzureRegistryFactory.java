package io.harness.delegate.beans.azure.registry;

import static java.lang.String.format;

import io.harness.exception.InvalidArgumentsException;

public class AzureRegistryFactory {
  private AzureRegistryFactory() {}

  public static AzureRegistry getAzureRegistry(AzureRegistryType type) {
    switch (type) {
      case ACR:
        return new AzureContainerRegistry();
      case DOCKER_HUB_PUBLIC:
        return new AzureDockerHubPublicRegistry();
      case DOCKER_HUB_PRIVATE:
        return new AzureDockerHubPrivateRegistry();
      case ARTIFACTORY_PRIVATE_REGISTRY:
        return new AzurePrivateRegistry();
      default:
        throw new InvalidArgumentsException(format("Unsupported Azure Docker Registry type: %s", type));
    }
  }
}
