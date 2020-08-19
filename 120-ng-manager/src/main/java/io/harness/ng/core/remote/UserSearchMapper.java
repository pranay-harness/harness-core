package io.harness.ng.core.remote;

import io.harness.ng.core.dto.UserSearchDTO;
import lombok.experimental.UtilityClass;
import software.wings.beans.User;

@UtilityClass
public class UserSearchMapper {
  public static UserSearchDTO writeDTO(User user) {
    return UserSearchDTO.builder().name(user.getName()).email(user.getEmail()).uuid(user.getUuid()).build();
  }
}
