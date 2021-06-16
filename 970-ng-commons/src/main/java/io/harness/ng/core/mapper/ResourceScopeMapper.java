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
}
