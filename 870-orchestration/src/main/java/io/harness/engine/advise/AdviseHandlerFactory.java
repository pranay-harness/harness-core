package io.harness.engine.advise;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.advise.handlers.EndPlanAdviserResponseHandler;
import io.harness.engine.advise.handlers.IgnoreFailureAdviseHandler;
import io.harness.engine.advise.handlers.InterventionWaitAdviserResponseHandler;
import io.harness.engine.advise.handlers.MarkSuccessAdviseHandler;
import io.harness.engine.advise.handlers.NextStepHandler;
import io.harness.engine.advise.handlers.RetryAdviserResponseHandler;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.advisers.AdviseType;

import com.google.inject.Inject;

@OwnedBy(CDC)
public class AdviseHandlerFactory {
  @Inject private NextStepHandler nextStepHandler;
  @Inject private RetryAdviserResponseHandler retryAdviseHandler;
  @Inject private EndPlanAdviserResponseHandler endPlanAdviseHandler;
  @Inject private InterventionWaitAdviserResponseHandler interventionWaitAdviseHandler;
  @Inject private MarkSuccessAdviseHandler markSuccessAdviseHandler;
  @Inject private IgnoreFailureAdviseHandler ignoreFailureAdviseHandler;

  public AdviserResponseHandler obtainHandler(AdviseType adviseType) {
    switch (adviseType) {
      case NEXT_STEP:
        return nextStepHandler;
      case RETRY:
        return retryAdviseHandler;
      case INTERVENTION_WAIT:
        return interventionWaitAdviseHandler;
      case END_PLAN:
        return endPlanAdviseHandler;
      case MARK_SUCCESS:
        return markSuccessAdviseHandler;
      case IGNORE_FAILURE:
        return ignoreFailureAdviseHandler;
      default:
        throw new InvalidRequestException("No handler Present for advise type: " + adviseType);
    }
  }
}
