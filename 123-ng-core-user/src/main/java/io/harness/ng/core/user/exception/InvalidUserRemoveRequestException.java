/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.user.exception;

import static io.harness.eraro.ErrorCode.REQUEST_PROCESSING_INTERRUPTED;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.beans.Scope;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public class InvalidUserRemoveRequestException extends WingsException {
  private static final String MESSAGE_ARG = "message";
  private final List<String> lastAdminScopes;

  public InvalidUserRemoveRequestException(String message, List<Scope> lastAdminScopes) {
    super(message, null, REQUEST_PROCESSING_INTERRUPTED, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
    this.lastAdminScopes = getScopesList(lastAdminScopes);
  }

  public static String getExceptionMessageForUserRemove(List<Scope> scopes) {
    List<Scope> orgAndProjectScopes =
        scopes.stream()
            .filter(scope -> scope.getProjectIdentifier() != null || scope.getOrgIdentifier() != null)
            .collect(toList());
    boolean isAccountScopePresent = orgAndProjectScopes.size() < scopes.size();
    long numProjects = scopes.stream().filter(scope -> scope.getProjectIdentifier() != null).count();
    long numOrgs = scopes.stream()
                       .filter(scope -> scope.getOrgIdentifier() != null && scope.getProjectIdentifier() == null)
                       .count();

    // Message: // Error msg: Can not remove user. User is last remaining admin in projectCount projects and orgCount
    // Response message organizations. ResponseMessage: Projects = [orgId/projectId], Oganizations = [orgId]

    String numProjectStringComponent =
        numProjects > 0 ? String.format("%s project%s, ", numProjects, numProjects > 1 ? "s" : "") : "";
    String numOrgStringComponent =
        numOrgs > 0 ? String.format("%s organization%s, ", numOrgs, numOrgs > 1 ? "s" : "") : "";

    String errorMsg = String.format("Can't remove user. User is last remaining admin in %s%s%s",
        isAccountScopePresent ? "account, " : "", numOrgStringComponent, numProjectStringComponent);
    errorMsg = StringUtils.removeEnd(errorMsg, ", ");
    errorMsg += ".";

    List<String> projectIds = new ArrayList<>();
    List<String> orgIds = new ArrayList<>();
    if (orgAndProjectScopes.size() <= 5) {
      for (Scope scope : orgAndProjectScopes.subList(0, Math.min(orgAndProjectScopes.size(), 5))) {
        if (scope.getProjectIdentifier() != null) {
          projectIds.add(String.format("%s/%s", scope.getOrgIdentifier(), scope.getProjectIdentifier()));
        } else {
          orgIds.add(scope.getOrgIdentifier());
        }
      }

      if (!orgIds.isEmpty()) {
        errorMsg +=
            String.format(" Organizations: [%s%s], ", String.join(", ", orgIds), numOrgs > orgIds.size() ? "..." : "");
      }
      if (!projectIds.isEmpty()) {
        errorMsg += String.format(
            " Projects: [%s%s], ", String.join(", ", projectIds), numProjects > projectIds.size() ? "..." : "");
      }
      errorMsg = StringUtils.removeEnd(errorMsg, ", ");
      errorMsg += ".";
    }
    return errorMsg;
  }

  public List<String> getScopesList(List<Scope> lastAdminScopes) {
    List<String> scopesStrings = new ArrayList<>();
    for (Scope scope : lastAdminScopes) {
      if (isNotBlank(scope.getProjectIdentifier())) {
        scopesStrings.add(String.format("%s/%s", scope.getOrgIdentifier(), scope.getProjectIdentifier()));
      } else if (isNotBlank(scope.getOrgIdentifier())) {
        scopesStrings.add(String.format("%s", scope.getOrgIdentifier()));
      }
    }
    return scopesStrings;
  }
}
