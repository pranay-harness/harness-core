package software.wings.infra;

import com.fasterxml.jackson.annotation.JsonIgnore;
import software.wings.beans.InfrastructureMapping;

public interface InfraMappingInfrastructureProvider extends CloudProviderInfrastructure {
  @JsonIgnore InfrastructureMapping getInfraMapping();

  @JsonIgnore Class<? extends InfrastructureMapping> getMappingClass();
}
