package io.harness.encryption;

import static io.harness.data.structure.HasPredicate.hasSome;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ScopeHelper {
  public static Scope getScope(String accountId, String orgId, String projectId) {
    if (hasSome(projectId)) {
      return Scope.PROJECT;
    } else if (hasSome(orgId)) {
      return Scope.ORG;
    } else if (hasSome(accountId)) {
      return Scope.ACCOUNT;
    }
    return Scope.UNKNOWN;
  }

  public static String getScopeMessageForLogs(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    StringBuilder scopeMessage = new StringBuilder(32);
    scopeMessage.append("account ").append(accountIdentifier);
    if (orgIdentifier != null) {
      scopeMessage.append(", org ").append(orgIdentifier);
    }
    if (projectIdentifier != null) {
      scopeMessage.append(", project ").append(projectIdentifier);
    }
    return scopeMessage.toString();
  }
}
