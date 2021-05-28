package io.harness.engine.events;

import static io.harness.pms.events.PmsEventFrameworkConstants.ORCHESTRATION_EVENT_PRODUCER;
import static io.harness.pms.events.PmsEventFrameworkConstants.SERVICE_NAME;

import io.harness.OrchestrationModuleConfig;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.logging.AutoLogContext;
import io.harness.observer.Subject;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEventLog;
import io.harness.pms.sdk.core.events.OrchestrationSubject;
import io.harness.pms.sdk.core.registries.OrchestrationEventHandlerRegistry;
import io.harness.pms.utils.PmsConstants;
import io.harness.queue.QueuePublisher;
import io.harness.repositories.orchestrationEventLog.OrchestrationEventLogRepository;
import io.harness.serializer.ProtoUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import java.sql.Date;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
@Singleton
public class OrchestrationEventEmitter {
  @Inject private OrchestrationModuleConfig configuration;

  @Inject private OrchestrationEventHandlerRegistry handlerRegistry;
  @Inject private QueuePublisher<OrchestrationEvent> orchestrationEventQueue;
  @Inject private OrchestrationEventLogRepository orchestrationEventLogRepository;
  @Inject @Named(ORCHESTRATION_EVENT_PRODUCER) private Producer eventProducer;

  @Getter @Setter Subject<OrchestrationEventLogHandler> orchestrationEventLogSubjectSubject = new Subject<>();

  public void emitEvent(OrchestrationEvent event) {
    try (AutoLogContext ignore = event.autoLogContext()) {
      OrchestrationSubject subject = new OrchestrationSubject();
      Set<OrchestrationEventHandler> handlers = handlerRegistry.obtain(event.getEventType());
      subject.registerAll(handlers);
      subject.handleEventSync(event);
      populateEventLog(event);
      String serviceName = event.getNodeExecutionProto() == null
          ? PmsConstants.INTERNAL_SERVICE_NAME
          : event.getNodeExecutionProto().getNode().getServiceName();

      if (configuration.isUseRedisForEvents()) {
        eventProducer.send(Message.newBuilder()
                               .putAllMetadata(ImmutableMap.of(SERVICE_NAME, serviceName))
                               .setData(buildProtoEvent(event))
                               .build());
        return;
      }

      if (event.getNodeExecutionProto() == null) {
        orchestrationEventQueue.send(Collections.singletonList(PmsConstants.INTERNAL_SERVICE_NAME), event);
      } else {
        orchestrationEventQueue.send(Collections.singletonList(serviceName), event);
        // For calling event handlers in PMS, create a one level clone of the event and then emit
        if (!serviceName.equals(PmsConstants.INTERNAL_SERVICE_NAME)) {
          orchestrationEventQueue.send(Collections.singletonList(PmsConstants.INTERNAL_SERVICE_NAME),
              OrchestrationEvent.builder()
                  .ambiance(event.getAmbiance())
                  .nodeExecutionProto(event.getNodeExecutionProto())
                  .eventType(event.getEventType())
                  .build());
        }
      }
    } catch (Exception ex) {
      log.error("Failed to create orchestration event", ex);
      throw ex;
    }
  }

  private ByteString buildProtoEvent(OrchestrationEvent event) {
    io.harness.pms.contracts.execution.events.OrchestrationEvent.Builder protoEvent =
        io.harness.pms.contracts.execution.events.OrchestrationEvent.newBuilder()
            .setEventType(event.getEventType())
            .setCreatedAt(ProtoUtils.unixMillisToTimestamp(System.currentTimeMillis()));
    if (event.getAmbiance() != null) {
      protoEvent.setAmbiance(event.getAmbiance());
    }
    if (event.getNodeExecutionProto() != null) {
      protoEvent.setNodeExecution(event.getNodeExecutionProto());
    }
    return protoEvent.build().toByteString();
  }

  private void populateEventLog(OrchestrationEvent event) {
    if (event.getEventType() == OrchestrationEventType.NODE_EXECUTION_UPDATE
        || event.getEventType() == OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE
        || event.getEventType() == OrchestrationEventType.PLAN_EXECUTION_STATUS_UPDATE) {
      OrchestrationEventLog orchestrationEventLog = orchestrationEventLogRepository.save(
          OrchestrationEventLog.builder()
              .createdAt(System.currentTimeMillis())
              .nodeExecutionId(event.getNodeExecutionProto() == null ? null : event.getNodeExecutionProto().getUuid())
              .orchestrationEventType(event.getEventType())
              .planExecutionId(event.getAmbiance().getPlanExecutionId())
              .validUntil(Date.from(OffsetDateTime.now().plus(Duration.ofDays(14)).toInstant()))
              .build());
      orchestrationEventLogSubjectSubject.fireInform(OrchestrationEventLogHandler::handleLog, orchestrationEventLog);
    }
  }
}
