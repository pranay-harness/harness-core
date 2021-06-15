package io.harness.ng.apikeys.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.apikeys.dto.TokenDTO;
import io.harness.ng.apikeys.entities.ApiKey;
import io.harness.ng.apikeys.service.api.ApiKeyService;
import io.harness.persistence.HPersistence;

import groovy.util.logging.Slf4j;
import javax.inject.Inject;

@Slf4j
@OwnedBy(PL)
public class ApiKeyServiceImpl implements ApiKeyService {
  @Inject private HPersistence hPersistence;

  private ApiKey getOrCreateApiKeyForToken(String accountIdentifier, TokenDTO tokenDTO) {
    ApiKey apiKey;
    if (isNotEmpty(tokenDTO.getApiKeyIdentifier())) {
      apiKey = hPersistence.get(ApiKey.class, tokenDTO.getApiKeyIdentifier());
    } else {
      apiKey = tokenDTO.createAPIKey(accountIdentifier);
      hPersistence.save(apiKey);
    }
    return apiKey;
  }

  @Override
  public String createToken(String accountIdentifier, TokenDTO tokenDTO) {
    ApiKey apiKey = getOrCreateApiKeyForToken(accountIdentifier, tokenDTO);

    return null;
  }
}
