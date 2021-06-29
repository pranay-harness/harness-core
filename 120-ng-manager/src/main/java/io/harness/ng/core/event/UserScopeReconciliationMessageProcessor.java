package io.harness.ng.core.event;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.beans.Scope.of;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ORGANIZATION_ENTITY;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.PROJECT_ENTITY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.organization.OrganizationEntityChangeDTO;
import io.harness.eventsframework.entity_crud.project.ProjectEntityChangeDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.user.UserMembershipUpdateSource;
import io.harness.ng.core.user.service.NgUserService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
@Singleton
public class UserScopeReconciliationMessageProcessor implements MessageProcessor {
  private final NgUserService ngUserService;

  @Inject
  public UserScopeReconciliationMessageProcessor(NgUserService ngUserService) {
    this.ngUserService = ngUserService;
  }

  @Override
  public boolean processMessage(Message message) {
    if (message != null && message.hasMessage()) {
      Map<String, String> metadataMap = message.getMessage().getMetadataMap();
      if (metadataMap != null && metadataMap.get(ENTITY_TYPE) != null) {
        String entityType = metadataMap.get(ENTITY_TYPE);
        switch (entityType) {
          case ORGANIZATION_ENTITY:
            return processOrganizationBasedUserChangeEvent(message);
          case PROJECT_ENTITY:
            return processProjectBasedUserChangeEvent(message);
          default:
        }
      }
    }
    return true;
  }

  private boolean processOrganizationBasedUserChangeEvent(Message message) {
    OrganizationEntityChangeDTO organizationEntityChangeDTO;
    try {
      organizationEntityChangeDTO = OrganizationEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      throw new InvalidRequestException(
          String.format("Exception in unpacking EntityChangeDTO for key %s", message.getId()), e);
    }
    String action = message.getMessage().getMetadataMap().get(ACTION);
    if (DELETE_ACTION.equals(action)) {
      return processOrganizationBasedUserDeleteEvent(organizationEntityChangeDTO);
    }
    return true;
  }

  private boolean processOrganizationBasedUserDeleteEvent(OrganizationEntityChangeDTO organizationEntityChangeDTO) {
    String accountIdentifier = organizationEntityChangeDTO.getAccountIdentifier();
    String id = organizationEntityChangeDTO.getIdentifier();
    Scope scope = of(accountIdentifier, id, null);
    List<String> userIds = ngUserService.listUserIds(scope);
    AtomicBoolean success = new AtomicBoolean(true);
    userIds.forEach(userId -> {
      if (!ngUserService.removeUserFromScope(userId, scope, UserMembershipUpdateSource.SYSTEM)) {
        log.error(String.format(
            "Delete operation failed for users with accountIdentifier %s and identifier %s", accountIdentifier, id));
        success.set(false);
      }
    });
    if (success.get()) {
      log.info(String.format("Successfully completed deletion for users having accountIdentifier %s and identifier %s",
          accountIdentifier, id));
    }
    return success.get();
  }

  private boolean processProjectBasedUserChangeEvent(Message message) {
    ProjectEntityChangeDTO projectEntityChangeDTO;
    try {
      projectEntityChangeDTO = ProjectEntityChangeDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      throw new InvalidRequestException(
          String.format("Exception is unpacking EntityChangeDTO for key %s", message.getId()), e);
    }
    String action = message.getMessage().getMetadataMap().get(ACTION);
    if (DELETE_ACTION.equals(action)) {
      return processProjectBasedUserDeleteEvent(projectEntityChangeDTO);
    }
    return true;
  }

  private boolean processProjectBasedUserDeleteEvent(ProjectEntityChangeDTO projectEntityChangeDTO) {
    String accountIdentifier = projectEntityChangeDTO.getAccountIdentifier();
    String orgIdentifier = projectEntityChangeDTO.getOrgIdentifier();
    String id = projectEntityChangeDTO.getIdentifier();
    Scope scope = of(accountIdentifier, orgIdentifier, id);
    List<String> userIds = ngUserService.listUserIds(scope);
    AtomicBoolean success = new AtomicBoolean(true);
    userIds.forEach(userId -> {
      if (!ngUserService.removeUserFromScope(userId, scope, UserMembershipUpdateSource.SYSTEM)) {
        log.error(String.format(
            "Delete operation failed for users with accountIdentifier %s, orgIdentifier %s and identifier %s",
            accountIdentifier, orgIdentifier, id));
        success.set(false);
      }
    });
    if (success.get()) {
      log.info(String.format(
          "Successfully completed deletion for users having accountIdentifier %s, orgIdentifier %s and identifier %s",
          accountIdentifier, orgIdentifier, id));
    }
    return success.get();
  }
}