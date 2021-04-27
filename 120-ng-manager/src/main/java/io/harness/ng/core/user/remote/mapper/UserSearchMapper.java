package io.harness.ng.core.user.remote.mapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.dto.UserSearchDTO;
import io.harness.ng.core.user.UserInfo;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PL)
public class UserSearchMapper {
  public static UserSearchDTO writeDTO(UserInfo user) {
    return UserSearchDTO.builder().name(user.getName()).email(user.getEmail()).uuid(user.getUuid()).build();
  }
}
