package io.harness.notification.mapper;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.NotificationRequest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.notification.dtos.UserGroup;
import io.harness.notification.dtos.UserGroup.UserGroupBuilder;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class NotificationUserGroupMapper {
  public List<NotificationRequest.UserGroup> toProto(List<UserGroup> userGroups) {
    List<NotificationRequest.UserGroup> protoUserGroups = new ArrayList<>();
    if (userGroups != null) {
      userGroups.forEach(ug -> {
        NotificationRequest.UserGroup.Builder ugBuilder =
            NotificationRequest.UserGroup.newBuilder().setIdentifier(ug.getIdentifier());
        if (isNotEmpty(ug.getOrgIdentifier())) {
          ugBuilder.setOrgIdentifier(ug.getOrgIdentifier());
        }
        if (isNotEmpty(ug.getProjectIdentifier())) {
          ugBuilder.setProjectIdentifier(ug.getProjectIdentifier());
        }
        protoUserGroups.add(ugBuilder.build());
      });
    }
    return protoUserGroups;
  }

  public List<UserGroup> toEntity(List<NotificationRequest.UserGroup> userGroups) {
    List<UserGroup> entityUserGroups = new ArrayList<>();
    if (userGroups != null) {
      userGroups.forEach(ug -> {
        UserGroupBuilder ugBuilder = UserGroup.builder().identifier(ug.getIdentifier());
        if (isNotEmpty(ug.getOrgIdentifier())) {
          ugBuilder.orgIdentifier(ug.getOrgIdentifier());
        }
        if (isNotEmpty(ug.getProjectIdentifier())) {
          ugBuilder.projectIdentifier(ug.getProjectIdentifier());
        }
        entityUserGroups.add(ugBuilder.build());
      });
    }
    return entityUserGroups;
  }
}