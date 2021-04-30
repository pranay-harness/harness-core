package io.harness.ng.core.user.service.impl;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CREATE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.UPDATE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.USER_ENTITY;
import static io.harness.ng.core.user.UserMembershipUpdateSource.SYSTEM;

import static org.apache.commons.lang3.StringUtils.stripToNull;

import io.harness.account.AccountClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.schemas.user.UserDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.event.MessageListener;
import io.harness.ng.core.user.entities.UserMembership.Scope;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.remote.client.RestClientUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PL)
@Slf4j
@Singleton
public class UserEntityCrudStreamListener implements MessageListener {
  private static final String ACCOUNT_VIEWER_ROLE_IDENTIFIER = "_account_viewer";
  private static final String ACCOUNT_ADMIN_ROLE_IDENTIFIER = "_account_admin";
  private final NgUserService ngUserService;
  private final AccountClient accountClient;

  @Inject
  public UserEntityCrudStreamListener(NgUserService ngUserService, AccountClient accountClient) {
    this.ngUserService = ngUserService;
    this.accountClient = accountClient;
  }

  @Override
  public boolean handleMessage(Message message) {
    if (message != null && message.hasMessage()) {
      Map<String, String> metadataMap = message.getMessage().getMetadataMap();
      if (metadataMap != null && metadataMap.get(ENTITY_TYPE) != null && metadataMap.get(ACTION) != null) {
        String entityType = metadataMap.get(ENTITY_TYPE);
        if (USER_ENTITY.equals(entityType)) {
          UserDTO userDTO;
          try {
            userDTO = UserDTO.parseFrom(message.getMessage().getData());
          } catch (InvalidProtocolBufferException e) {
            throw new InvalidRequestException(
                String.format("Exception in unpacking UserDTO for key %s", message.getId()), e);
          }
          return handleMessage(userDTO, metadataMap.get(ACTION));
        }
      }
    }
    return true;
  }

  private boolean handleMessage(UserDTO userDTO, String action) {
    String userId = stripToNull(userDTO.getUserId());
    if (Objects.isNull(userId)) {
      log.error("UserId can't be null in the event consumed from entity crud stream");
      return true;
    }
    switch (action) {
      case CREATE_ACTION:
        return handleUserAddedAction(userDTO);
      case UPDATE_ACTION:
        return handleUserUpdateAction(userDTO);
      case DELETE_ACTION:
        return handleUserDeletedAction(userDTO);
      default:
        return true;
    }
  }

  private boolean handleUserAddedAction(UserDTO userDTO) {
    if (userDTO.getNewAccountsUserAddedToList() != null) {
      addUserToAccounts(userDTO.getUserId(), userDTO.getNewAccountsUserAddedToList());
    }
    return true;
  }

  private boolean handleUserUpdateAction(UserDTO userDTO) {
    if (userDTO.getNewAccountsUserAddedToList() != null) {
      addUserToAccounts(userDTO.getUserId(), userDTO.getNewAccountsUserAddedToList());
    }
    if (userDTO.getAccountsUserRemovedFromList() != null) {
      userDTO.getAccountsUserRemovedFromList().forEach(
          accountIdentifier -> ngUserService.removeUserFromAccount(userDTO.getUserId(), accountIdentifier));
    }
    return true;
  }

  private boolean handleUserDeletedAction(UserDTO userDTO) {
    ngUserService.removeUser(userDTO.getUserId());
    return true;
  }

  private void addUserToAccounts(String userId, List<String> newAccountsUserAddedTo) {
    newAccountsUserAddedTo.forEach(accountIdentifier -> {
      List<String> accountAdmins = RestClientUtils.getResponse(accountClient.getAccountAdmins(accountIdentifier));
      String roleIdentifier = accountAdmins != null && accountAdmins.contains(userId) ? ACCOUNT_ADMIN_ROLE_IDENTIFIER
                                                                                      : ACCOUNT_VIEWER_ROLE_IDENTIFIER;
      Scope scope = Scope.builder().accountIdentifier(accountIdentifier).build();
      ngUserService.addUserToScope(userId, scope, roleIdentifier, SYSTEM);
    });
  }
}
