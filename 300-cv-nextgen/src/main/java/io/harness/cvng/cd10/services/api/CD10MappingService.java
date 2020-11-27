package io.harness.cvng.cd10.services.api;

import io.harness.cvng.cd10.beans.CD10MappingsDTO;

public interface CD10MappingService {
  void create(String accountId, CD10MappingsDTO envMappingRequest);
  CD10MappingsDTO list(String accountId, String orgIdentifier, String projectIdentifier);
}
