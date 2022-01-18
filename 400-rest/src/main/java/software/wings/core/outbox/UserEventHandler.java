/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.core.outbox;

import static io.harness.ng.core.ResourceConstants.LABEL_KEY_RESOURCE_NAME;
import static io.harness.ng.core.ResourceConstants.LABEL_KEY_USER_ID;

import static software.wings.core.events.Login2FAEvent.LOGIN2FA;
import static software.wings.core.events.LoginEvent.LOGIN;
import static software.wings.core.events.UnsuccessfulLoginEvent.UNSUCCESSFUL_LOGIN;

import io.harness.ModuleType;
import io.harness.audit.Action;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.beans.AuthenticationInfoDTO;
import io.harness.audit.beans.ResourceDTO;
import io.harness.audit.beans.ResourceScopeDTO;
import io.harness.audit.client.api.AuditClientService;
import io.harness.context.GlobalContext;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.AccountScope;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;
import io.harness.security.dto.UserPrincipal;

import com.google.inject.Inject;

public class UserEventHandler implements OutboxEventHandler {
  private final AuditClientService auditClientService;

  @Inject
  public UserEventHandler(AuditClientService auditClientService) {
    this.auditClientService = auditClientService;
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    try {
      switch (outboxEvent.getEventType()) {
        case LOGIN:
          return handleLoginEvent(outboxEvent);
        case LOGIN2FA:
          return handleLogin2faEvent(outboxEvent);
        case UNSUCCESSFUL_LOGIN:
          return handleUnsuccessfulLoginEvent(outboxEvent);
        default:
          throw new InvalidArgumentsException(String.format("Not supported event type %s", outboxEvent.getEventType()));
      }
    } catch (Exception exception) {
      return false;
    }
  }

  private boolean handleLoginEvent(OutboxEvent outboxEvent) {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    String accountIdentifier = ((AccountScope) outboxEvent.getResourceScope()).getAccountIdentifier();
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.LOGIN)
                                .module(ModuleType.CORE)
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();
    AuthenticationInfoDTO authenticationInfoDTO = getAuthenticationInfoForLoginEvent(accountIdentifier, auditEntry);
    return auditClientService.publishAudit(auditEntry, authenticationInfoDTO, globalContext);
  }

  private boolean handleLogin2faEvent(OutboxEvent outboxEvent) {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    String accountIdentifier = ((AccountScope) outboxEvent.getResourceScope()).getAccountIdentifier();
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.LOGIN2FA)
                                .module(ModuleType.CORE)
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();
    AuthenticationInfoDTO authenticationInfoDTO = getAuthenticationInfoForLoginEvent(accountIdentifier, auditEntry);
    return auditClientService.publishAudit(auditEntry, authenticationInfoDTO, globalContext);
  }

  private boolean handleUnsuccessfulLoginEvent(OutboxEvent outboxEvent) {
    GlobalContext globalContext = outboxEvent.getGlobalContext();
    String accountIdentifier = ((AccountScope) outboxEvent.getResourceScope()).getAccountIdentifier();
    AuditEntry auditEntry = AuditEntry.builder()
                                .action(Action.UNSUCCESSFUL_LOGIN)
                                .module(ModuleType.CORE)
                                .timestamp(outboxEvent.getCreatedAt())
                                .resource(ResourceDTO.fromResource(outboxEvent.getResource()))
                                .resourceScope(ResourceScopeDTO.fromResourceScope(outboxEvent.getResourceScope()))
                                .insertId(outboxEvent.getId())
                                .build();
    AuthenticationInfoDTO authenticationInfoDTO = getAuthenticationInfoForLoginEvent(accountIdentifier, auditEntry);
    return auditClientService.publishAudit(auditEntry, authenticationInfoDTO, globalContext);
  }

  private AuthenticationInfoDTO getAuthenticationInfoForLoginEvent(String accountIdentifier, AuditEntry auditEntry) {
    ResourceDTO resource = auditEntry.getResource();
    String email = resource.getIdentifier();
    String userId = resource.getLabels().get(LABEL_KEY_USER_ID);
    String username = resource.getLabels().get(LABEL_KEY_RESOURCE_NAME);
    UserPrincipal principal = new UserPrincipal(userId, email, username, accountIdentifier);
    return AuthenticationInfoDTO.fromSecurityPrincipal(principal);
  }
}
