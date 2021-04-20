package io.harness.pms.sdk.core.adviser.ignore;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.advisers.NextStepAdvise;
import io.harness.pms.contracts.advisers.NextStepAdvise.Builder;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.core.adviser.AdvisingEvent;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.serializer.KryoSerializer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Collections;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
public class IgnoreAdviser implements Adviser {
  public static final AdviserType ADVISER_TYPE =
      AdviserType.newBuilder().setType(OrchestrationAdviserTypes.IGNORE.name()).build();

  @Inject private KryoSerializer kryoSerializer;

  @Override
  public AdviserResponse onAdviseEvent(AdvisingEvent advisingEvent) {
    IgnoreAdviserParameters parameters = extractParameters(advisingEvent);
    Builder builder = NextStepAdvise.newBuilder();
    // Change here
    if (EmptyPredicate.isNotEmpty(parameters.getNextNodeId())) {
      builder.setNextNodeId(parameters.getNextNodeId());
    }
    builder.setToStatus(Status.IGNORE_FAILED);
    return AdviserResponse.newBuilder().setNextStepAdvise(builder.build()).setType(AdviseType.NEXT_STEP).build();
  }

  @Override
  public boolean canAdvise(AdvisingEvent advisingEvent) {
    IgnoreAdviserParameters parameters = extractParameters(advisingEvent);
    boolean canAdvise = StatusUtils.brokeStatuses().contains(advisingEvent.getToStatus());
    FailureInfo failureInfo = advisingEvent.getNodeExecution().getFailureInfo();
    if (failureInfo != null && !isEmpty(failureInfo.getFailureTypesList())) {
      return canAdvise
          && !Collections.disjoint(parameters.getApplicableFailureTypes(), failureInfo.getFailureTypesList());
    }
    return canAdvise;
  }

  @NotNull
  private IgnoreAdviserParameters extractParameters(AdvisingEvent advisingEvent) {
    return (IgnoreAdviserParameters) Preconditions.checkNotNull(
        kryoSerializer.asObject(advisingEvent.getAdviserParameters()));
  }
}
