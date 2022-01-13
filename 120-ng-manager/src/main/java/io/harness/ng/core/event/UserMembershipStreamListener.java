/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.event;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;

import static org.apache.commons.lang3.StringUtils.stripToNull;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.schemas.usermembership.Scope;
import io.harness.eventsframework.schemas.usermembership.UserMembershipDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.api.UserGroupService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
@Singleton
public class UserMembershipStreamListener implements MessageListener {
  private final UserGroupService userGroupService;

  @Inject
  public UserMembershipStreamListener(UserGroupService userGroupService) {
    this.userGroupService = userGroupService;
  }

  @Override
  public boolean handleMessage(Message message) {
    if (message != null && message.hasMessage()) {
      UserMembershipDTO userMembershipDTO;
      try {
        userMembershipDTO = UserMembershipDTO.parseFrom(message.getMessage().getData());
      } catch (InvalidProtocolBufferException e) {
        throw new InvalidRequestException(
            String.format("Exception in unpacking UserMembership for key %s", message.getId()), e);
      }
      return processUserMembershipEvent(userMembershipDTO);
    }
    return true;
  }

  private boolean processUserMembershipEvent(UserMembershipDTO userMembershipDTO) {
    String action = userMembershipDTO.getAction();
    if (DELETE_ACTION.equals(action)) {
      return processDeleteEvent(userMembershipDTO);
    }
    return true;
  }

  private boolean processDeleteEvent(UserMembershipDTO userMembershipDTO) {
    Scope scope = userMembershipDTO.getScope();
    userGroupService.removeMemberAll(stripToNull(scope.getAccountIdentifier()), stripToNull(scope.getOrgIdentifier()),
        stripToNull(scope.getProjectIdentifier()), stripToNull(userMembershipDTO.getUserId()));
    return true;
  }
}
