package io.harness.engine.events;

import static io.harness.pms.events.PmsEventFrameworkConstants.SERVICE_NAME;

import io.harness.OrchestrationModuleConfig;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.utils.OrchestrationEventsFrameworkUtils;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.logging.AutoLogContext;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.utils.PmsConstants;
import io.harness.queue.QueuePublisher;
import io.harness.serializer.ProtoUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
@Singleton
public class OrchestrationEventEmitter {
  @Inject private OrchestrationModuleConfig configuration;
  @Inject private OrchestrationEventsFrameworkUtils eventsFrameworkUtils;
  @Inject private QueuePublisher<OrchestrationEvent> orchestrationEventQueue;

  public void emitEvent(OrchestrationEvent event) {
    try (AutoLogContext ignore = event.autoLogContext()) {
      String serviceName = event.getServiceName() == null ? PmsConstants.INTERNAL_SERVICE_NAME : event.getServiceName();
      if (configuration.isUseRedisForEvents()) {
        emitViaEventsFramework(event, serviceName);
        return;
      }
      orchestrationEventQueue.send(Collections.singletonList(serviceName), event);
    } catch (Exception ex) {
      log.error("Failed to create orchestration event", ex);
      throw ex;
    }
  }

  private void emitViaEventsFramework(OrchestrationEvent event, String serviceName) {
    Producer producer = eventsFrameworkUtils.obtainProducerForOrchestrationEvent(serviceName);
    producer.send(Message.newBuilder()
                      .putAllMetadata(ImmutableMap.of(SERVICE_NAME, serviceName))
                      .setData(buildProtoEvent(event))
                      .build());
  }

  private ByteString buildProtoEvent(OrchestrationEvent event) {
    io.harness.pms.contracts.execution.events.OrchestrationEvent.Builder protoEvent =
        io.harness.pms.contracts.execution.events.OrchestrationEvent.newBuilder()
            .setEventType(event.getEventType())
            .setCreatedAt(ProtoUtils.unixMillisToTimestamp(System.currentTimeMillis()));
    if (event.getAmbiance() != null) {
      protoEvent.setAmbiance(event.getAmbiance());
    }
    if (event.getStatus() != null) {
      protoEvent.setStatus(event.getStatus());
    }
    if (event.getResolvedStepParameters() != null) {
      protoEvent.setStepParameters(ByteString.copyFromUtf8(event.getResolvedStepParameters()));
    }
    if (event.getServiceName() != null) {
      protoEvent.setServiceName(event.getServiceName());
    }
    return protoEvent.build().toByteString();
  }
}
