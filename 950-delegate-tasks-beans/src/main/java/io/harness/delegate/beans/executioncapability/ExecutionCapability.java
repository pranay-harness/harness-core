package io.harness.delegate.beans.executioncapability;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.time.Duration;

@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._955_DELEGATE_BEANS)
public interface ExecutionCapability {
  enum EvaluationMode { MANAGER, AGENT }

  EvaluationMode evaluationMode();

  CapabilityType getCapabilityType();
  String fetchCapabilityBasis();

  /**
   * Should return the maximal period for which the existing successful check of the capability can be considered as
   * valid. Applicable to capabilities with Evaluation Mode AGENT.
   */
  Duration getMaxValidityPeriod();
  /**
   * Should return the period that should pass until the capability check should be validated again. Applicable to
   * capabilities with Evaluation Mode AGENT.
   */
  Duration getPeriodUntilNextValidation();
}
