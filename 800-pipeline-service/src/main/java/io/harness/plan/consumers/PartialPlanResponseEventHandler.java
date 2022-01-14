package io.harness.plan.consumers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.async.plan.PartialPlanCreatorResponseData;
import io.harness.pms.contracts.plan.PartialPlanResponse;
import io.harness.pms.sdk.execution.events.PmsCommonsBaseEventHandler;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class PartialPlanResponseEventHandler implements PmsCommonsBaseEventHandler<PartialPlanResponse> {
  @Inject WaitNotifyEngine waitNotifyEngine;

  @Override
  public void handleEvent(PartialPlanResponse event, Map<String, String> metadataMap, long timestamp) {
    waitNotifyEngine.doneWith(
        event.getNotifyId(), PartialPlanCreatorResponseData.builder().partialPlanResponse(event).build());
  }
}