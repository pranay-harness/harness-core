package io.harness.encryption;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ScopeHelper {
  public static Scope getScope(String accountId, String orgId, String projectId) {
    if (isNotEmpty(projectId)) {
      return Scope.PROJECT;
    } else if (isNotEmpty(orgId)) {
      return Scope.ORG;
    } else if (isNotEmpty(accountId)) {
      return Scope.ACCOUNT;
    }
    return null;
  }
}
