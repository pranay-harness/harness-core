package io.harness.ng.core.outbox;

import static io.harness.audit.ResourceTypeConstants.ORGANIZATION;
import static io.harness.audit.ResourceTypeConstants.PROJECT;

import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NextGenOutboxEventHandler implements OutboxEventHandler {
  private final OrgEventHandler orgEventHandler;
  private final ProjectEventHandler projectEventHandler;

  @Inject
  public NextGenOutboxEventHandler(OrgEventHandler orgEventHandler, ProjectEventHandler projectEventHandler) {
    this.orgEventHandler = orgEventHandler;
    this.projectEventHandler = projectEventHandler;
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    try {
      switch (outboxEvent.getResource().getType()) {
        case ORGANIZATION:
          return orgEventHandler.handle(outboxEvent);
        case PROJECT:
          return projectEventHandler.handle(outboxEvent);
        default:
          return true;
      }
    } catch (Exception IOException) {
      return false;
    }
  }
}
