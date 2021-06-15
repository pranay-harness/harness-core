package io.harness.ng.apikeys.dto;

import io.harness.ng.apikeys.beans.ApiKeyType;
import io.harness.ng.apikeys.entities.ApiKey;
import io.harness.ng.apikeys.entities.UserApiKey;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName("USER")
@EqualsAndHashCode(callSuper = true)
public class UserTokenDTO extends TokenDTO {
  String userIdentifier;

  @Override
  public ApiKeyType getType() {
    return ApiKeyType.USER;
  }

  @Override
  public ApiKey createAPIKey(String accountId) {
    UserApiKey userApiKey = UserApiKey.builder()
                                .userIdentifier(userIdentifier)
                                .accountId(accountId)
                                .name(getName())
                                .scope(getScope())
                                .build();
    userApiKey.setUuid(getApiKeyIdentifier());
    return userApiKey;
  }
}
