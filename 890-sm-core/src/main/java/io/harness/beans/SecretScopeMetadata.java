/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

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
