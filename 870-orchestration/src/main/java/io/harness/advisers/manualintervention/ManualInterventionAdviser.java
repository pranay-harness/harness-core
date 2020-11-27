package io.harness.advisers.manualintervention;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.pms.execution.Status.INTERVENTION_WAITING;

import io.harness.StatusUtils;
import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdvisingEvent;
import io.harness.adviser.OrchestrationAdviserTypes;
import io.harness.adviser.advise.InterventionWaitAdvise;
import io.harness.pms.advisers.AdviserType;
import io.harness.serializer.KryoSerializer;
import io.harness.state.io.FailureInfo;

import com.google.inject.Inject;
import java.util.Collections;

public class ManualInterventionAdviser implements Adviser {
  public static final AdviserType ADVISER_TYPE =
      AdviserType.newBuilder().setType(OrchestrationAdviserTypes.MANUAL_INTERVENTION.name()).build();

  @Inject private KryoSerializer kryoSerializer;

  @Override
  public Advise onAdviseEvent(AdvisingEvent advisingEvent) {
    return InterventionWaitAdvise.builder().build();
  }

  @Override
  public boolean canAdvise(AdvisingEvent advisingEvent) {
    boolean canAdvise = StatusUtils.brokeStatuses().contains(advisingEvent.getToStatus())
        && advisingEvent.getFromStatus() != INTERVENTION_WAITING;
    ManualInterventionAdviserParameters parameters = extractParameters(advisingEvent);
    FailureInfo failureInfo = advisingEvent.getFailureInfo();
    if (failureInfo != null && !isEmpty(failureInfo.getFailureTypes())) {
      return canAdvise && !Collections.disjoint(parameters.getApplicableFailureTypes(), failureInfo.getFailureTypes());
    }
    return canAdvise;
  }

  private ManualInterventionAdviserParameters extractParameters(AdvisingEvent advisingEvent) {
    byte[] adviserParameters = advisingEvent.getAdviserParameters();
    if (adviserParameters == null || adviserParameters.length == 0) {
      return null;
    }
    return (ManualInterventionAdviserParameters) kryoSerializer.asObject(adviserParameters);
  }
}
