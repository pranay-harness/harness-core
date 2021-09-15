/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.scopes.core;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
public class Scope {
  public static final String PATH_DELIMITER = "/";

  @NotNull ScopeLevel level;
  @NotEmpty String instanceId;
  Scope parentScope;

  @Override
  public String toString() {
    String identifier = PATH_DELIMITER + level.getResourceType() + PATH_DELIMITER + instanceId;
    if (parentScope != null) {
      return parentScope.toString().concat(identifier);
    }
    return identifier;
  }
}
