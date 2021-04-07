package io.harness.ng.core.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.ResourceTypeConstants.ORGANIZATION;
import static io.harness.audit.ResourceTypeConstants.PROJECT;
import static io.harness.audit.ResourceTypeConstants.RESOURCE_GROUP;
import static io.harness.audit.ResourceTypeConstants.USER_GROUP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;
import io.harness.resourcegroup.outbox.ResourceGroupEventHandler;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class NextGenOutboxEventHandler implements OutboxEventHandler {
  private final OrganizationEventHandler organizationEventHandler;
  private final ProjectEventHandler projectEventHandler;
  private final UserGroupEventHandler userGroupEventHandler;
  private final ResourceGroupEventHandler resourceGroupEventHandler;

  @Inject
  public NextGenOutboxEventHandler(OrganizationEventHandler organizationEventHandler,
      ProjectEventHandler projectEventHandler, UserGroupEventHandler userGroupEventHandler,
      ResourceGroupEventHandler resourceGroupEventHandler) {
    this.organizationEventHandler = organizationEventHandler;
    this.projectEventHandler = projectEventHandler;
    this.userGroupEventHandler = userGroupEventHandler;
    this.resourceGroupEventHandler = resourceGroupEventHandler;
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    try {
      switch (outboxEvent.getResource().getType()) {
        case ORGANIZATION:
          return organizationEventHandler.handle(outboxEvent);
        case PROJECT:
          return projectEventHandler.handle(outboxEvent);
        case USER_GROUP:
          return userGroupEventHandler.handle(outboxEvent);
        case RESOURCE_GROUP:
          return resourceGroupEventHandler.handle(outboxEvent);
        default:
          return true;
      }
    } catch (Exception exception) {
      log.error(
          String.format("Unexpected error occurred during handling event of type %s", outboxEvent.getEventType()));
      return false;
    }
  }
}
