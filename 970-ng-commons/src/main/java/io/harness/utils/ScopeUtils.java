/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.utils;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.beans.ScopeLevel.ORGANIZATION;
import static io.harness.beans.ScopeLevel.PROJECT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.beans.ScopeLevel;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
@OwnedBy(PL)
public class ScopeUtils {
  private static final String PROJECT_ADDR = "%s/%s/%s";
  private static final String ORG_ADDR = "%s/%s";
  private static final String ACCOUNT_ADDR = "%s";

  public static String toString(Scope scope) {
    return ScopeUtils.toString(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier());
  }

  public static String toString(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (!StringUtils.isBlank(projectIdentifier)) {
      return String.format(PROJECT_ADDR, accountIdentifier, orgIdentifier, projectIdentifier);
    }
    if (!StringUtils.isBlank(orgIdentifier)) {
      return String.format(ORG_ADDR, accountIdentifier, orgIdentifier);
    }
    return String.format(ACCOUNT_ADDR, accountIdentifier);
  }

  public static ScopeLevel getMostSignificantScope(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return ScopeLevel.of(accountIdentifier, orgIdentifier, projectIdentifier);
  }

  public static ScopeLevel getMostSignificantScope(Scope scope) {
    return ScopeLevel.of(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier());
  }

  public static ScopeLevel getImmediateNextScope(String accountIdentifier, String orgIdentifier) {
    ScopeLevel scopeLevel = ScopeLevel.of(accountIdentifier, orgIdentifier, null);
    if (scopeLevel.equals(ORGANIZATION)) {
      return PROJECT;
    }
    return ORGANIZATION;
  }

  public static boolean isAccountScope(Scope scope) {
    return isAccountScope(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier());
  }

  public static boolean isAccountScope(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return accountIdentifier != null && orgIdentifier == null && projectIdentifier == null;
  }

  public static boolean isOrganizationScope(Scope scope) {
    return isOrganizationScope(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier());
  }

  public static boolean isOrganizationScope(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return accountIdentifier != null && orgIdentifier != null && projectIdentifier == null;
  }

  public static boolean isProjectScope(Scope scope) {
    return isProjectScope(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier());
  }

  public static boolean isProjectScope(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return accountIdentifier != null && orgIdentifier != null && projectIdentifier != null;
  }
}
