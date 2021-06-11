package io.harness.engine.events;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.pms.events.PmsEventFrameworkConstants.SERVICE_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.utils.OrchestrationEventsFrameworkUtils;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.execution.events.OrchestrationEvent;
import io.harness.pms.execution.utils.OrchestrationEventUtils;
import io.harness.pms.utils.PmsConstants;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
@Singleton
public class OrchestrationEventEmitter {
  @Inject private OrchestrationEventsFrameworkUtils eventsFrameworkUtils;

  public void emitEvent(OrchestrationEvent event) {
    try (AutoLogContext ignore = OrchestrationEventUtils.obtainLogContext(event)) {
      String serviceName =
          isEmpty(event.getServiceName()) ? PmsConstants.INTERNAL_SERVICE_NAME : event.getServiceName();
      emitViaEventsFramework(event, serviceName);
    } catch (Exception ex) {
      log.error("Failed to create orchestration event", ex);
      throw ex;
    }
  }

  private void emitViaEventsFramework(OrchestrationEvent event, String serviceName) {
    Producer producer = eventsFrameworkUtils.obtainProducerForOrchestrationEvent(serviceName);
    producer.send(Message.newBuilder()
                      .putAllMetadata(ImmutableMap.of(SERVICE_NAME, serviceName))
                      .setData(event.toByteString())
                      .build());
  }
}
