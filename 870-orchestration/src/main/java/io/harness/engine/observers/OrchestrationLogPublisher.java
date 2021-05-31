package io.harness.engine.observers;

import io.harness.engine.events.OrchestrationEventLogHandler;
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
public class OrchestrationLogPublisher implements NodeUpdateObserver {
  @Inject private OrchestrationEventLogRepository orchestrationEventLogRepository;
  @Getter @Setter Subject<OrchestrationEventLogHandler> orchestrationEventLogSubjectSubject = new Subject<>();

  @Override
  public void onNodeUpdate(NodeUpdateInfo nodeUpdateInfo) {
    OrchestrationEventLog orchestrationEventLog = orchestrationEventLogRepository.save(
        OrchestrationEventLog.builder()
            .createdAt(System.currentTimeMillis())
            .nodeExecutionId(nodeUpdateInfo.getNodeExecutionId())
            .orchestrationEventType(OrchestrationEventType.NODE_EXECUTION_UPDATE)
            .planExecutionId(nodeUpdateInfo.getPlanExecutionId())
            .validUntil(Date.from(OffsetDateTime.now().plus(Duration.ofDays(14)).toInstant()))
            .build());
    orchestrationEventLogSubjectSubject.fireInform(OrchestrationEventLogHandler::handleLog, orchestrationEventLog);
  }
}
