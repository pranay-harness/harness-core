package io.harness.steps.resourcerestraint;

import static io.harness.pms.utils.PmsConstants.QUEUING_RC_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.shared.ResourceConstraint;
import io.harness.beans.shared.RestraintService;
import io.harness.distribution.constraint.Constraint.Strategy;
import io.harness.engine.observers.OrchestrationStartObserver;
import io.harness.engine.observers.beans.OrchestrationStartInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.execution.utils.AmbianceUtils;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class ResourceRestraintInitializer implements OrchestrationStartObserver {
  @Inject private RestraintService restraintService;

  @Override
  public void onStart(OrchestrationStartInfo orchestrationStartInfo) {
    final String accountId = AmbianceUtils.getAccountId(orchestrationStartInfo.getAmbiance());
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
      log.info("Resource Constraint already exist for name {}", QUEUING_RC_NAME);
    }
  }
}
