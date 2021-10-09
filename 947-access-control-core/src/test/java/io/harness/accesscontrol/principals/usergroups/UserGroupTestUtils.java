package io.harness.accesscontrol.principals.usergroups;

import static io.harness.accesscontrol.common.AccessControlTestUtils.getRandomString;

import io.harness.accesscontrol.principals.usergroups.persistence.UserGroupDBO;
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
public class UserGroupTestUtils {
  public static UserGroup buildUserGroup(@NotEmpty String scopeIdentifier) {
    return UserGroup.builder()
        .identifier(getRandomString(20))
        .scopeIdentifier(scopeIdentifier)
        .users(Sets.newHashSet(getRandomString(20), getRandomString(20), getRandomString(20)))
        .build();
  }

  public static UserGroupDBO buildUserGroupDBO(@NotEmpty String scopeIdentifier, int usersCount) {
    Set<String> users = new HashSet<>();
    int remainingUsers = usersCount;
    while (remainingUsers > 0) {
      users.add(getRandomString(10));
      remainingUsers--;
    }

    return UserGroupDBO.builder()
        .id(getRandomString(20))
        .name(getRandomString(20))
        .identifier(getRandomString(20))
        .scopeIdentifier(scopeIdentifier)
        .users(users)
        .build();
  }
}
