package io.harness.audit.beans;

import io.harness.exception.InvalidArgumentsException;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

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
        return Principal.builder().type(PrincipalType.USER).identifier(principal.getName()).build();
      case API_KEY:
        return Principal.builder().type(PrincipalType.API_KEY).identifier(principal.getName()).build();
      case SERVICE:
        return Principal.builder().type(PrincipalType.SYSTEM).identifier(String.valueOf(PrincipalType.SYSTEM)).build();
      default:
        throw new InvalidArgumentsException(String.format("Unknown principal type %s", principal.getType()));
    }
  }
}
