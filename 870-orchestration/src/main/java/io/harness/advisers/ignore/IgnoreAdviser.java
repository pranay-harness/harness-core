package io.harness.advisers.ignore;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.adviser.Adviser;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.OrchestrationAdviserTypes;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.advisers.AdviseType;
import io.harness.pms.advisers.AdviserResponse;
import io.harness.pms.advisers.AdviserType;
import io.harness.pms.advisers.NextStepAdvise;
import io.harness.pms.execution.failure.FailureInfo;
import io.harness.serializer.KryoSerializer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Collections;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Redesign
public class IgnoreAdviser implements Adviser {
  public static final AdviserType ADVISER_TYPE =
      AdviserType.newBuilder().setType(OrchestrationAdviserTypes.IGNORE.name()).build();

  @Inject private KryoSerializer kryoSerializer;

  @Override
  public AdviserResponse onAdviseEvent(AdvisingEvent advisingEvent) {
    IgnoreAdviserParameters parameters = extractParameters(advisingEvent);
    return AdviserResponse.newBuilder()
        .setNextStepAdvise(NextStepAdvise.newBuilder().setNextNodeId(parameters.getNextNodeId()).build())
        .setType(AdviseType.NEXT_STEP)
        .build();
  }

  @Override
  public boolean canAdvise(AdvisingEvent advisingEvent) {
    IgnoreAdviserParameters parameters = extractParameters(advisingEvent);
    FailureInfo failureInfo = advisingEvent.getFailureInfo();
    if (failureInfo != null && !isEmpty(failureInfo.getFailureTypesList())) {
      return !Collections.disjoint(parameters.getApplicableFailureTypes(), failureInfo.getFailureTypesList());
    }
    return true;
  }

  @NotNull
  private IgnoreAdviserParameters extractParameters(AdvisingEvent advisingEvent) {
    return (IgnoreAdviserParameters) Preconditions.checkNotNull(
        kryoSerializer.asObject(advisingEvent.getAdviserParameters()));
  }
}
