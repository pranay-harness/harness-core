/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.roles;

import static io.harness.accesscontrol.common.AccessControlTestUtils.getRandomString;

import io.harness.accesscontrol.roles.persistence.RoleDBO;
import io.harness.accesscontrol.scopes.TestScopeLevels;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Set;
import javax.validation.executable.ValidateOnExecution;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;

@UtilityClass
@ValidateOnExecution
@OwnedBy(HarnessTeam.PL)
public class RoleTestUtils {
  public static Role buildRole(@NotEmpty String scopeIdentifier) {
    return Role.builder()
        .identifier(getRandomString(20))
        .scopeIdentifier(scopeIdentifier)
        .allowedScopeLevels(Sets.newHashSet(TestScopeLevels.TEST_SCOPE.toString()))
        .permissions(Sets.newHashSet(getRandomString(20), getRandomString(20), getRandomString(20)))
        .build();
  }

  public static RoleDBO buildRoleRBO(@NotEmpty String scopeIdentifier, int permissionsCount) {
    Set<String> permissions = new HashSet<>();
    int remainingPermissions = permissionsCount;
    while (remainingPermissions > 0) {
      permissions.add(getRandomString(10));
      remainingPermissions--;
    }

    return RoleDBO.builder()
        .id(getRandomString(20))
        .identifier(getRandomString(20))
        .scopeIdentifier(scopeIdentifier)
        .allowedScopeLevels(Sets.newHashSet(TestScopeLevels.TEST_SCOPE.toString()))
        .permissions(permissions)
        .build();
  }
}
