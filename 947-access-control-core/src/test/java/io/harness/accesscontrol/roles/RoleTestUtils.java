package io.harness.accesscontrol.roles;

import static io.harness.accesscontrol.common.AccessControlTestUtils.getRandomString;

import io.harness.accesscontrol.scopes.TestScopeLevels;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.collect.Sets;
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
}
