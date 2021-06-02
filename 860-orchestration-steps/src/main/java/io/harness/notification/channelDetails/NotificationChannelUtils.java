package io.harness.notification.channelDetails;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class NotificationChannelUtils {
  io.harness.NotificationRequest.UserGroup getUserGroups(
      String identifier, String orgIdentifier, String projectIdentifier) {
    return io.harness.NotificationRequest.UserGroup.newBuilder()
        .setIdentifier(identifier)
        .setOrgIdentifier(orgIdentifier)
        .setProjectIdentifier(projectIdentifier)
        .build();
  }
}
