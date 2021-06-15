package io.harness.ng.apikeys.dto;

import io.harness.beans.Scope;
import io.harness.ng.apikeys.beans.ApiKeyType;
import io.harness.ng.apikeys.entities.ApiKey;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class TokenDTO {
  String name;
  String apiKeyIdentifier;
  Scope scope;
  @Nullable Long expireAfterInMillis;

  public abstract ApiKeyType getType();
  public abstract ApiKey createAPIKey(String accountId);
}
