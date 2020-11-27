package io.harness.cvng.cd10.beans;

import java.util.Collections;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CD10MappingsDTO {
  String accountId;
  String orgIdentifier;
  String projectIdentifier;
  Set<CD10EnvMappingDTO> envMappings;
  Set<CD10ServiceMappingDTO> serviceMappings;

  public Set<CD10ServiceMappingDTO> getServiceMappings() {
    if (serviceMappings == null) {
      return Collections.emptySet();
    }
    return serviceMappings;
  }

  public Set<CD10EnvMappingDTO> getEnvMappings() {
    if (envMappings == null) {
      return Collections.emptySet();
    }
    return envMappings;
  }
}
