package io.harness.ng.apikeys.entities;

import io.harness.ng.apikeys.beans.ApiKeyType;

import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserApiKey extends ApiKey {
  @NotNull String userIdentifier;

  @Override
  public ApiKeyType getType() {
    return ApiKeyType.USER;
  }
}
