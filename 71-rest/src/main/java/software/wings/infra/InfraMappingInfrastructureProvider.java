package software.wings.infra;

import software.wings.beans.InfrastructureMapping;

public interface InfraMappingInfrastructureProvider extends CloudProviderInfrastructure, FieldKeyValMapProvider {
  InfrastructureMapping getInfraMapping();

  Class<? extends InfrastructureMapping> getMappingClass();
}
