package io.harness.steps.resourcerestraint;

import static io.harness.pms.utils.PmsConstants.QUEUING_RC_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.shared.ResourceConstraint;
import io.harness.beans.shared.RestraintService;
import io.harness.distribution.constraint.Constraint.Strategy;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.plan.execution.SetupAbstractionKeys;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.SyncOrchestrationEventHandler;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class ResourceRestraintInitializer implements SyncOrchestrationEventHandler {
  @Inject private RestraintService restraintService;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    final String accountId = event.getAmbiance().getSetupAbstractionsMap().get(SetupAbstractionKeys.accountId);
    try {
      ResourceConstraint resourceConstraint = ResourceConstraint.builder()
                                                  .name(QUEUING_RC_NAME)
                                                  .accountId(accountId)
                                                  .capacity(1)
                                                  .harnessOwned(true)
                                                  .strategy(Strategy.FIFO)
                                                  .build();
      restraintService.save(resourceConstraint);
    } catch (InvalidRequestException e) {
      log.info("Resource Constraint Already exist for name {}", QUEUING_RC_NAME);
    }
  }
}
