/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.mapper;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.ResourceScope;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PL)
public class ResourceScopeMapper {
  public static ResourceScope getResourceScope(Scope scope) {
    if (isEmpty(scope.getOrgIdentifier())) {
      return new AccountScope(scope.getAccountIdentifier());
    } else if (isEmpty(scope.getProjectIdentifier())) {
      return new OrgScope(scope.getAccountIdentifier(), scope.getOrgIdentifier());
    }
    return new ProjectScope(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier());
  }

  public static Scope getScopeFromResourceScope(ResourceScope resourceScope) {
    if ("account".equals(resourceScope.getScope())) {
      return Scope.of(((AccountScope) resourceScope).getAccountIdentifier(), "", "");
    } else if ("org".equals(resourceScope.getScope())) {
      OrgScope orgScope = (OrgScope) resourceScope;
      return Scope.of(orgScope.getAccountIdentifier(), orgScope.getOrgIdentifier(), "");
    } else {
      ProjectScope projectScope = (ProjectScope) resourceScope;
      return Scope.of(
          projectScope.getAccountIdentifier(), projectScope.getOrgIdentifier(), projectScope.getProjectIdentifier());
    }
  }
}
