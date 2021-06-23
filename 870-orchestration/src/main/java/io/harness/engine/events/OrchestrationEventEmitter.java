package io.harness.engine.events;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.pms.commons.events.PmsEventSender;
import io.harness.logging.AutoLogContext;
import io.harness.pms.PmsFeatureFlagService;
import io.harness.pms.contracts.execution.events.OrchestrationEvent;
import io.harness.pms.events.base.PmsEventCategory;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.OrchestrationEventUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
@Singleton
public class OrchestrationEventEmitter {
  @Inject private PmsEventSender eventSender;
  @Inject private PmsFeatureFlagService pmsFeatureFlagService;

  public void emitEvent(OrchestrationEvent event) {
    try (AutoLogContext ignore = OrchestrationEventUtils.obtainLogContext(event)) {
      String serviceName =
          isEmpty(event.getServiceName()) ? ModuleType.PMS.name().toLowerCase() : event.getServiceName();
      String accountId = AmbianceUtils.getAccountId(event.getAmbiance());
      eventSender.sendEvent(event.toByteString(), PmsEventCategory.ORCHESTRATION_EVENT, serviceName, accountId, true);
    } catch (Exception ex) {
      log.error("Failed to create orchestration event", ex);
      throw ex;
    }
  }
}
