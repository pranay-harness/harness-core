package io.harness.utils;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
@OwnedBy(PL)
public class ScopeUtils {
  private static final String PROJECT_ADDR = "%s/%s/%s";
  private static final String ORG_ADDR = "%s/%s";
  private static final String ACCOUNT_ADDR = "%s";
  public static String toString(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (!StringUtils.isBlank(projectIdentifier)) {
      return String.format(PROJECT_ADDR, accountIdentifier, orgIdentifier, projectIdentifier);
    }
    if (!StringUtils.isBlank(orgIdentifier)) {
      return String.format(ORG_ADDR, accountIdentifier, orgIdentifier);
    }
    return String.format(ACCOUNT_ADDR, accountIdentifier);
  }

  public static Scope getMostSignificantScope(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return Scope.of(accountIdentifier, orgIdentifier, projectIdentifier);
  }
}
