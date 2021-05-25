package io.harness.pms.orchestrationevent;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.events.OrchestrationEventLogHandler;
import io.harness.engine.interrupts.statusupdate.NodeExecutionUpdate;
import io.harness.execution.NodeExecution;
import io.harness.observer.Subject;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEventLog;
import io.harness.repositories.orchestrationEventLog.OrchestrationEventLogRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.sql.Date;
import java.time.Duration;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class OrchestrationEventLogPublisher implements NodeExecutionUpdate {
  @Inject private OrchestrationEventLogRepository orchestrationEventLogRepository;
  @Getter @Setter Subject<OrchestrationEventLogHandler> orchestrationEventLogSubjectSubject = new Subject<>();
  @Override
  public void onUpdate(OrchestrationEventType eventType, NodeExecution nodeExecution) {
    if (eventType == OrchestrationEventType.NODE_EXECUTION_UPDATE
        || eventType == OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE
        || eventType == OrchestrationEventType.PLAN_EXECUTION_STATUS_UPDATE) {
      OrchestrationEventLog orchestrationEventLog = orchestrationEventLogRepository.save(
          OrchestrationEventLog.builder()
              .createdAt(System.currentTimeMillis())
              .nodeExecutionId(nodeExecution.getUuid())
              .orchestrationEventType(eventType)
              .planExecutionId(nodeExecution.getAmbiance().getPlanExecutionId())
              .validUntil(Date.from(OffsetDateTime.now().plus(Duration.ofDays(14)).toInstant()))
              .build());
      orchestrationEventLogSubjectSubject.fireInform(OrchestrationEventLogHandler::handleLog, orchestrationEventLog);
    }
  }
}
