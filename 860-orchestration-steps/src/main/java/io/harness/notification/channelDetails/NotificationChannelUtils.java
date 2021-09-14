/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.channelDetails;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.encryption.Scope;
import io.harness.utils.IdentifierRefHelper;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class NotificationChannelUtils {
  io.harness.NotificationRequest.UserGroup getUserGroups(
      String identifier, String accountId, String orgIdentifier, String projectIdentifier) {
    IdentifierRef identifierRef =
        IdentifierRefHelper.getIdentifierRef(identifier, accountId, orgIdentifier, projectIdentifier);

    if (identifierRef.getScope() == Scope.ACCOUNT) {
      return io.harness.NotificationRequest.UserGroup.newBuilder().setIdentifier(identifierRef.getIdentifier()).build();
    }
    if (identifierRef.getScope() == Scope.ORG) {
      return io.harness.NotificationRequest.UserGroup.newBuilder()
          .setIdentifier(identifierRef.getIdentifier())
          .setOrgIdentifier(orgIdentifier)
          .build();
    }
    return io.harness.NotificationRequest.UserGroup.newBuilder()
        .setIdentifier(identifierRef.getIdentifier())
        .setOrgIdentifier(orgIdentifier)
        .setProjectIdentifier(projectIdentifier)
        .build();
  }
}
