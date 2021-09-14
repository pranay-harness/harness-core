/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.executioncapability;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.time.Duration;

@TargetModule(HarnessModule._957_CG_BEANS)
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
