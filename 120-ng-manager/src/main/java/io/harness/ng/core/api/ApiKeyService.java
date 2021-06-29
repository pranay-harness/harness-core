package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.common.beans.ApiKeyType;
import io.harness.ng.core.dto.ApiKeyDTO;
import io.harness.ng.core.entities.ApiKey;

import java.util.List;

@OwnedBy(PL)
public interface ApiKeyService {
  ApiKeyDTO createApiKey(ApiKeyDTO apiKeyDTO);
  ApiKeyDTO updateApiKey(ApiKeyDTO apiKeyDTO);
  boolean deleteApiKey(String accountIdentifier, String orgIdentifier, String projectIdentifier, ApiKeyType apiKeyType,
      String parentIdentifier, String identifier);
  List<ApiKeyDTO> listApiKeys(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      ApiKeyType apiKeyType, String parentIdentifier, List<String> identifiers);
  ApiKey getApiKey(String accountIdentifier, String orgIdentifier, String projectIdentifier, ApiKeyType apiKeyType,
      String parentIdentifier, String identifier);
}
