package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.common.beans.ApiKeyType;
import io.harness.ng.core.dto.ApiKeyAggregateDTO;
import io.harness.ng.core.dto.ApiKeyDTO;
import io.harness.ng.core.dto.ApiKeyFilterDTO;
import io.harness.ng.core.entities.ApiKey;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;

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

  Map<String, Integer> getApiKeysPerParentIdentifier(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, ApiKeyType apiKeyType, List<String> parentIdentifier);

  PageResponse<ApiKeyAggregateDTO> listAggregateApiKeys(
      String accountIdentifier, Pageable pageable, ApiKeyFilterDTO filterDTO);

  ApiKeyAggregateDTO getApiKeyAggregateDTO(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      ApiKeyType apiKeyType, String parentIdentifier, String identifier);

  long deleteAllByParentIdentifier(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      ApiKeyType apiKeyType, String parentIdentifier);

  void validateParentIdentifier(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      ApiKeyType apiKeyType, String parentIdentifier);
}
