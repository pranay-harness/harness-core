package io.harness.ng.apikeys.dto;

import io.harness.ng.apikeys.beans.ApiKeyType;
import io.harness.ng.apikeys.entities.ApiKey;
import io.harness.ng.apikeys.entities.ServiceAccountApiKey;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName("SERVICE_ACCOUNT")
@EqualsAndHashCode(callSuper = true)
public class ServiceAccountTokenDTO extends TokenDTO {
  String serviceAccountIdentifier;

  @Override
  public ApiKeyType getType() {
    return ApiKeyType.SERVICE_ACCOUNT;
  }

  @Override
  public ApiKey createAPIKey(String accountId) {
    ServiceAccountApiKey serviceAccountApiKey = ServiceAccountApiKey.builder()
                                                    .serviceAccountIdentifier(serviceAccountIdentifier)
                                                    .accountId(accountId)
                                                    .name(getName())
                                                    .scope(getScope())
                                                    .build();
    serviceAccountApiKey.setUuid(getApiKeyIdentifier());
    return serviceAccountApiKey;
  }
}
