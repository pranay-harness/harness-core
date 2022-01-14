package io.harness.accesscontrol.principals.users.persistence;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.principals.users.User;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class UserDBOMapper {
  public static UserDBO toDBO(User object) {
    return UserDBO.builder()
        .identifier(object.getIdentifier())
        .scopeIdentifier(object.getScopeIdentifier())
        .createdAt(object.getCreatedAt())
        .lastModifiedAt(object.getLastModifiedAt())
        .version(object.getVersion())
        .build();
  }

  public static User fromDBO(UserDBO object) {
    return User.builder()
        .identifier(object.getIdentifier())
        .scopeIdentifier(object.getScopeIdentifier())
        .createdAt(object.getCreatedAt())
        .lastModifiedAt(object.getLastModifiedAt())
        .version(object.getVersion())
        .build();
  }
}
