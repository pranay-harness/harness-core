package io.harness.accesscontrol.commons.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.ResourceTypeConstants.ROLE;
import static io.harness.audit.ResourceTypeConstants.ROLE_ASSIGNMENT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
public class AccessControlEventHandler implements OutboxEventHandler {
  private final RoleEventHandler roleEventHandler;
  private final RoleAssignmentEventHandler roleAssignmentEventHandler;

  @Inject
  public AccessControlEventHandler(
      RoleEventHandler roleEventHandler, RoleAssignmentEventHandler roleAssignmentEventHandler) {
    this.roleEventHandler = roleEventHandler;
    this.roleAssignmentEventHandler = roleAssignmentEventHandler;
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    try {
      switch (outboxEvent.getResource().getType()) {
        case ROLE:
          return roleEventHandler.handle(outboxEvent);
        case ROLE_ASSIGNMENT:
          return roleAssignmentEventHandler.handle(outboxEvent);
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
