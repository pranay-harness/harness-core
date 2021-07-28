package io.harness.ng.core.user.remote.mapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.entities.UserMetadata;
import io.harness.ng.core.user.remote.dto.UserMetadataDTO;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PL)
public class UserMetadataMapper {
  public static UserMetadataDTO toDTO(UserInfo user) {
    return UserMetadataDTO.builder()
        .name(user.getName())
        .email(user.getEmail())
        .uuid(user.getUuid())
        .locked(user.isLocked())
        .build();
  }

  public static UserMetadataDTO toDTO(UserMetadata user) {
    return UserMetadataDTO.builder()
        .name(user.getName())
        .email(user.getEmail())
        .uuid(user.getUserId())
        .locked(Boolean.TRUE.equals(user.isLocked()))
        .build();
  }
}
