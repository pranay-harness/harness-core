package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.core.entities.ApiKey.DEFAULT_TTL_FOR_TOKEN;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.api.ApiKeyService;
import io.harness.ng.core.common.beans.ApiKeyType;
import io.harness.ng.core.dto.ApiKeyDTO;
import io.harness.ng.core.entities.ApiKey;
import io.harness.ng.core.events.ApiKeyCreateEvent;
import io.harness.ng.core.events.ApiKeyDeleteEvent;
import io.harness.ng.core.events.ApiKeyUpdateEvent;
import io.harness.ng.core.mapper.ApiKeyDTOMapper;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.ng.core.spring.ApiKeyRepository;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PL)
public class ApiKeyServiceImpl implements ApiKeyService {
  @Inject private ApiKeyRepository apiKeyRepository;
  @Inject private OutboxService outboxService;

  @Override
  public ApiKeyDTO createApiKey(ApiKeyDTO apiKeyDTO) {
    Optional<ApiKey> optionalApiKey =
        apiKeyRepository
            .findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndApiKeyTypeAndParentIdentifierAndIdentifier(
                apiKeyDTO.getAccountIdentifier(), apiKeyDTO.getOrgIdentifier(), apiKeyDTO.getProjectIdentifier(),
                apiKeyDTO.getApiKeyType(), apiKeyDTO.getParentIdentifier(), apiKeyDTO.getIdentifier());
    Preconditions.checkState(
        !optionalApiKey.isPresent(), "Duplicate api key present in scope for identifier: " + apiKeyDTO.getIdentifier());
    ApiKey apiKey = ApiKeyDTOMapper.getApiKeyFromDTO(apiKeyDTO);
    apiKey = apiKeyRepository.save(apiKey);
    ApiKeyDTO savedDTO = ApiKeyDTOMapper.getDTOFromApiKey(apiKey);
    outboxService.save(new ApiKeyCreateEvent(savedDTO));
    return savedDTO;
  }

  @Override
  public ApiKeyDTO updateApiKey(ApiKeyDTO apiKeyDTO) {
    Optional<ApiKey> optionalApiKey =
        apiKeyRepository
            .findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndApiKeyTypeAndParentIdentifierAndIdentifier(
                apiKeyDTO.getAccountIdentifier(), apiKeyDTO.getOrgIdentifier(), apiKeyDTO.getProjectIdentifier(),
                apiKeyDTO.getApiKeyType(), apiKeyDTO.getParentIdentifier(), apiKeyDTO.getIdentifier());
    Preconditions.checkState(
        optionalApiKey.isPresent(), "Api key not present in scope for identifier: " + apiKeyDTO.getIdentifier());
    ApiKey existingKey = optionalApiKey.get();
    ApiKeyDTO existingDTO = ApiKeyDTOMapper.getDTOFromApiKey(existingKey);
    existingKey.setDefaultTimeToExpireToken(apiKeyDTO.getDefaultTimeToExpireToken() == null
            ? DEFAULT_TTL_FOR_TOKEN
            : apiKeyDTO.getDefaultTimeToExpireToken());
    existingKey = apiKeyRepository.save(existingKey);
    ApiKeyDTO savedDTO = ApiKeyDTOMapper.getDTOFromApiKey(existingKey);
    outboxService.save(new ApiKeyUpdateEvent(existingDTO, savedDTO));
    return savedDTO;
  }

  @Override
  public boolean deleteApiKey(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      ApiKeyType apiKeyType, String parentIdentifier, String identifier) {
    Optional<ApiKey> optionalApiKey =
        apiKeyRepository
            .findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndApiKeyTypeAndParentIdentifierAndIdentifier(
                accountIdentifier, orgIdentifier, projectIdentifier, apiKeyType, parentIdentifier, identifier);
    Preconditions.checkState(optionalApiKey.isPresent(), "Api key not present in scope for identifier: ", identifier);
    ApiKeyDTO existingDTO = ApiKeyDTOMapper.getDTOFromApiKey(optionalApiKey.get());
    long deleted =
        apiKeyRepository
            .deleteByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndApiKeyTypeAndParentIdentifierAndIdentifier(
                accountIdentifier, orgIdentifier, projectIdentifier, apiKeyType, parentIdentifier, identifier);
    if (deleted > 0) {
      outboxService.save(new ApiKeyDeleteEvent(existingDTO));
      return true;
    } else {
      return false;
    }
  }

  @Override
  public List<ApiKeyDTO> listApiKeys(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      ApiKeyType apiKeyType, String parentIdentifier, List<String> identifiers) {
    List<ApiKey> apiKeys;
    if (isEmpty(identifiers)) {
      apiKeys = apiKeyRepository
                    .findAllByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndApiKeyTypeAndParentIdentifier(
                        accountIdentifier, orgIdentifier, projectIdentifier, apiKeyType, parentIdentifier);
    } else {
      apiKeys =
          apiKeyRepository
              .findAllByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndApiKeyTypeAndParentIdentifierAndIdentifierIn(
                  accountIdentifier, orgIdentifier, projectIdentifier, apiKeyType, parentIdentifier, identifiers);
    }
    List<ApiKeyDTO> apiKeyDTOS = new ArrayList<>();
    apiKeys.forEach(apiKey -> apiKeyDTOS.add(ApiKeyDTOMapper.getDTOFromApiKey(apiKey)));
    return apiKeyDTOS;
  }

  @Override
  public ApiKey getApiKey(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      ApiKeyType apiKeyType, String parentIdentifier, String identifier) {
    Optional<ApiKey> optionalApiKey =
        apiKeyRepository
            .findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndApiKeyTypeAndParentIdentifierAndIdentifier(
                accountIdentifier, orgIdentifier, projectIdentifier, apiKeyType, parentIdentifier, identifier);
    Preconditions.checkState(optionalApiKey.isPresent(), "Api key not present in scope for identifier: ", identifier);
    return optionalApiKey.get();
  }
}
