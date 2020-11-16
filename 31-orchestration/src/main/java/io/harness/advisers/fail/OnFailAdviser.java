package io.harness.advisers.fail;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.base.Preconditions;

import io.harness.StatusUtils;
import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.OrchestrationAdviserTypes;
import io.harness.adviser.advise.NextStepAdvise;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.advisers.AdviserType;
import io.harness.state.io.FailureInfo;

import java.util.Collections;

@OwnedBy(CDC)
@Redesign
public class OnFailAdviser implements Adviser<OnFailAdviserParameters> {
  public static final AdviserType ADVISER_TYPE =
      AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_FAIL.name()).build();

  @Override
  public Advise onAdviseEvent(AdvisingEvent<OnFailAdviserParameters> advisingEvent) {
    OnFailAdviserParameters parameters = Preconditions.checkNotNull(advisingEvent.getAdviserParameters());
    return NextStepAdvise.builder().nextNodeId(parameters.getNextNodeId()).build();
  }

  @Override
  public boolean canAdvise(AdvisingEvent<OnFailAdviserParameters> advisingEvent) {
    OnFailAdviserParameters parameters = advisingEvent.getAdviserParameters();
    if (parameters.getNextNodeId() == null) {
      return false;
    }
    boolean canAdvise = StatusUtils.brokeStatuses().contains(advisingEvent.getToStatus());
    FailureInfo failureInfo = advisingEvent.getFailureInfo();
    if (failureInfo != null && !isEmpty(failureInfo.getFailureTypes())) {
      return canAdvise && !Collections.disjoint(parameters.getApplicableFailureTypes(), failureInfo.getFailureTypes());
    }
    return canAdvise;
  }
}
