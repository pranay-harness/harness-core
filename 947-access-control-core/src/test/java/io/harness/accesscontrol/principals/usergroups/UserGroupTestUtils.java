package io.harness.accesscontrol.principals.usergroups;

import static io.harness.accesscontrol.common.AccessControlTestUtils.getRandomString;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.collect.Sets;
import javax.validation.executable.ValidateOnExecution;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;

@UtilityClass
@ValidateOnExecution
@OwnedBy(HarnessTeam.PL)
public class UserGroupTestUtils {
  public static UserGroup buildUserGroup(@NotEmpty String scopeIdentifier) {
    return UserGroup.builder()
        .identifier(getRandomString(20))
        .scopeIdentifier(scopeIdentifier)
        .users(Sets.newHashSet(getRandomString(20), getRandomString(20), getRandomString(20)))
        .build();
  }
}
