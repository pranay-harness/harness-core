package io.harness.delegate.beans.executioncapability;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.time.Duration;

@TargetModule(Module._955_DELEGATE_BEANS)
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
