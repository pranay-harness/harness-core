package io.harness.engine.events;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.pms.events.PmsEventFrameworkConstants.PIPELINE_MONITORING_ENABLED;
import static io.harness.pms.events.PmsEventFrameworkConstants.SERVICE_NAME;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.engine.utils.OrchestrationEventsFrameworkUtils;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.logging.AutoLogContext;
import io.harness.pms.PmsFeatureFlagService;
import io.harness.pms.contracts.execution.events.OrchestrationEvent;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.OrchestrationEventUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
@Singleton
public class OrchestrationEventEmitter {
  @Inject private OrchestrationEventsFrameworkUtils eventsFrameworkUtils;
  @Inject private PmsFeatureFlagService pmsFeatureFlagService;

  public void emitEvent(OrchestrationEvent event) {
    try (AutoLogContext ignore = OrchestrationEventUtils.obtainLogContext(event)) {
      String serviceName =
          isEmpty(event.getServiceName()) ? ModuleType.PMS.name().toLowerCase() : event.getServiceName();
      emitViaEventsFramework(event, serviceName);
    } catch (Exception ex) {
      log.error("Failed to create orchestration event", ex);
      throw ex;
    }
  }

  private void emitViaEventsFramework(OrchestrationEvent event, String serviceName) {
    Producer producer = eventsFrameworkUtils.obtainProducerForOrchestrationEvent(serviceName);
    Map<String, String> metadataMap = new HashMap<>();
    metadataMap.put(PIPELINE_MONITORING_ENABLED, "false");
    metadataMap.put(SERVICE_NAME, serviceName);
    if (pmsFeatureFlagService.isEnabled(
            AmbianceUtils.getAccountId(event.getAmbiance()), FeatureName.PIPELINE_MONITORING)) {
      metadataMap.put(PIPELINE_MONITORING_ENABLED, "true");
    }
    producer.send(Message.newBuilder().putAllMetadata(metadataMap).setData(event.toByteString()).build());
  }
}
