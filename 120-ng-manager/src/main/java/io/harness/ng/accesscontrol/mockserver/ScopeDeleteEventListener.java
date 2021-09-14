/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.accesscontrol.mockserver;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACCOUNT_ENTITY;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ORGANIZATION_ENTITY;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.PROJECT_ENTITY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.account.AccountEntityChangeDTO;
import io.harness.eventsframework.entity_crud.organization.OrganizationEntityChangeDTO;
import io.harness.eventsframework.entity_crud.project.ProjectEntityChangeDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.event.MessageListener;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;

@OwnedBy(PL)
public class ScopeDeleteEventListener implements MessageListener {
  MockRoleAssignmentService mockRoleAssignmentService;

  @Inject
  public ScopeDeleteEventListener(MockRoleAssignmentService mockRoleAssignmentService) {
    this.mockRoleAssignmentService = mockRoleAssignmentService;
  }

  @Override
  public boolean handleMessage(Message message) {
    if (message != null && message.hasMessage()) {
      Map<String, String> metadataMap = message.getMessage().getMetadataMap();
      if (metadataMap != null && metadataMap.get(ENTITY_TYPE) != null && metadataMap.get(ACTION) != null) {
        String action = metadataMap.get(ACTION);
        if (!action.equals(DELETE_ACTION)) {
          return true;
        }
        String entityType = metadataMap.get(ENTITY_TYPE);
        switch (entityType) {
          case ACCOUNT_ENTITY:
            return handleAccountDeleteEvent(message);
          case ORGANIZATION_ENTITY:
            return handleOrgDeleteEvent(message);
          case PROJECT_ENTITY:
            return handleProjectDeleteEvent(message);
          default:
            return true;
        }
      }
    }
    return true;
  }

  private boolean handleAccountDeleteEvent(Message message) {
    AccountEntityChangeDTO acocuntEntityChangeDTO;
    try {
      acocuntEntityChangeDTO = AccountEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      throw new InvalidRequestException(
          String.format("Exception in unpacking EntityChangeDTO for key %s", message.getId()), e);
    }
    mockRoleAssignmentService.deleteAll(acocuntEntityChangeDTO.getAccountId(), null, null);
    return true;
  }

  private boolean handleOrgDeleteEvent(Message message) {
    OrganizationEntityChangeDTO organizationEntityChangeDTO;
    try {
      organizationEntityChangeDTO = OrganizationEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      throw new InvalidRequestException(
          String.format("Exception in unpacking EntityChangeDTO for key %s", message.getId()), e);
    }
    mockRoleAssignmentService.deleteAll(
        organizationEntityChangeDTO.getAccountIdentifier(), organizationEntityChangeDTO.getIdentifier(), null);
    return true;
  }

  private boolean handleProjectDeleteEvent(Message message) {
    ProjectEntityChangeDTO projectEntityChangeDTO;
    try {
      projectEntityChangeDTO = ProjectEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      throw new InvalidRequestException(
          String.format("Exception in unpacking EntityChangeDTO for key %s", message.getId()), e);
    }
    mockRoleAssignmentService.deleteAll(projectEntityChangeDTO.getAccountIdentifier(),
        projectEntityChangeDTO.getOrgIdentifier(), projectEntityChangeDTO.getIdentifier());
    return true;
  }
}
