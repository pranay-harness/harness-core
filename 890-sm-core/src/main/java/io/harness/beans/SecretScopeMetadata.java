package io.harness.beans;

import software.wings.security.ScopedEntity;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode
public class SecretScopeMetadata {
  String secretId;
  @NotNull ScopedEntity secretScopes;
  boolean inheritScopesFromSM;
  ScopedEntity secretsManagerScopes;

  public ScopedEntity getCalculatedScopes() {
    if (inheritScopesFromSM) {
      return secretsManagerScopes;
    }
    return secretScopes;
  }
}
