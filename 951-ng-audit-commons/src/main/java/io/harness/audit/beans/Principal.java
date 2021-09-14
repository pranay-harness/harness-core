/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.audit.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;
import io.harness.security.dto.UserPrincipal;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@OwnedBy(PL)
@Data
@Builder
@FieldNameConstants(innerTypeName = "PrincipalKeys")
public class Principal {
  @NotNull PrincipalType type;
  @NotNull String identifier;

  public static Principal fromSecurityPrincipal(io.harness.security.dto.Principal principal) {
    if (principal == null) {
      return null;
    }
    switch (principal.getType()) {
      case USER:
        return Principal.builder().type(PrincipalType.USER).identifier(((UserPrincipal) principal).getEmail()).build();
      case API_KEY:
        return Principal.builder().type(PrincipalType.API_KEY).identifier(principal.getName()).build();
      case SERVICE:
        return Principal.builder().type(PrincipalType.SYSTEM).identifier(String.valueOf(PrincipalType.SYSTEM)).build();
      case SERVICE_ACCOUNT:
        return Principal.builder().type(PrincipalType.SERVICE_ACCOUNT).identifier(principal.getName()).build();
      default:
        throw new InvalidArgumentsException(String.format("Unknown principal type %s", principal.getType()));
    }
  }
}
