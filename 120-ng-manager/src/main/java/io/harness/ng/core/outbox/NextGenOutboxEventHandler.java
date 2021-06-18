package io.harness.ng.core.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.ResourceTypeConstants.CONNECTOR;
import static io.harness.audit.ResourceTypeConstants.DELEGATE_CONFIGURATION;
import static io.harness.audit.ResourceTypeConstants.ORGANIZATION;
import static io.harness.audit.ResourceTypeConstants.PROJECT;
import static io.harness.audit.ResourceTypeConstants.SECRET;
import static io.harness.audit.ResourceTypeConstants.SERVICE;
import static io.harness.audit.ResourceTypeConstants.SERVICE_ACCOUNT;
import static io.harness.audit.ResourceTypeConstants.USER;
import static io.harness.audit.ResourceTypeConstants.USER_GROUP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.events.ConnectorEventHandler;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class NextGenOutboxEventHandler implements OutboxEventHandler {
  private final OrganizationEventHandler organizationEventHandler;
  private final ProjectEventHandler projectEventHandler;
  private final SecretEventHandler secretEventHandler;
  private final UserGroupEventHandler userGroupEventHandler;
  private final UserEventHandler userEventHandler;
  private final DelegateProfileEventHandler delegateProfileEventHandler;
  private final ServiceOutBoxEventHandler serviceOutBoxEventHandler;
  private final ServiceAccountEventHandler serviceAccountEventHandler;
  private final ConnectorEventHandler connectorEventHandler;

  @Inject
  public NextGenOutboxEventHandler(OrganizationEventHandler organizationEventHandler,
      ProjectEventHandler projectEventHandler, UserGroupEventHandler userGroupEventHandler,
      SecretEventHandler secretEventHandler, UserEventHandler userEventHandler,
      DelegateProfileEventHandler delegateProfileEventHandler, ServiceOutBoxEventHandler serviceOutBoxEventHandler,
      ServiceAccountEventHandler serviceAccountEventHandler, ConnectorEventHandler connectorEventHandler) {
    this.organizationEventHandler = organizationEventHandler;
    this.projectEventHandler = projectEventHandler;
    this.userGroupEventHandler = userGroupEventHandler;
    this.secretEventHandler = secretEventHandler;
    this.userEventHandler = userEventHandler;
    this.delegateProfileEventHandler = delegateProfileEventHandler;
    this.serviceOutBoxEventHandler = serviceOutBoxEventHandler;
    this.serviceAccountEventHandler = serviceAccountEventHandler;
    this.connectorEventHandler = connectorEventHandler;
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    try {
      // TODO {karan} remove extra lowercase cases after some days
      switch (outboxEvent.getResource().getType()) {
        case ORGANIZATION:
        case "organization":
          return organizationEventHandler.handle(outboxEvent);
        case PROJECT:
        case "project":
          return projectEventHandler.handle(outboxEvent);
        case USER_GROUP:
        case "usergroup":
          return userGroupEventHandler.handle(outboxEvent);
        case SECRET:
        case "secret":
          return secretEventHandler.handle(outboxEvent);
        case USER:
        case "user":
          return userEventHandler.handle(outboxEvent);
        case DELEGATE_CONFIGURATION:
        case "delegateconfiguration":
          return delegateProfileEventHandler.handle(outboxEvent);
        case SERVICE:
          return serviceOutBoxEventHandler.handle(outboxEvent);
        case SERVICE_ACCOUNT:
        case "serviceaccount":
          return serviceAccountEventHandler.handle(outboxEvent);
        case CONNECTOR:
        case "connector":
          return connectorEventHandler.handle(outboxEvent);
        default:
          return false;
      }
    } catch (Exception exception) {
      log.error(
          String.format("Unexpected error occurred during handling event of type %s", outboxEvent.getEventType()));
      return false;
    }
  }
}
