package io.harness.advisers.ignore;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

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
public class IgnoreAdviser implements Adviser<IgnoreAdviserParameters> {
  public static final AdviserType ADVISER_TYPE =
      AdviserType.newBuilder().setType(OrchestrationAdviserTypes.IGNORE.name()).build();

  @Override
  public Advise onAdviseEvent(AdvisingEvent<IgnoreAdviserParameters> advisingEvent) {
    IgnoreAdviserParameters parameters = advisingEvent.getAdviserParameters();
    return NextStepAdvise.builder().nextNodeId(parameters.getNextNodeId()).build();
  }

  @Override
  public boolean canAdvise(AdvisingEvent<IgnoreAdviserParameters> advisingEvent) {
    IgnoreAdviserParameters parameters = advisingEvent.getAdviserParameters();
    FailureInfo failureInfo = advisingEvent.getFailureInfo();
    if (failureInfo != null && !isEmpty(failureInfo.getFailureTypes())) {
      return !Collections.disjoint(parameters.getApplicableFailureTypes(), failureInfo.getFailureTypes());
    }
    return true;
  }
}
