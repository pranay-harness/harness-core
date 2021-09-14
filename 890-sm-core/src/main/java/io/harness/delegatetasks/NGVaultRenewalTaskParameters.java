/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegatetasks;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;

import software.wings.beans.BaseVaultConfig;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@OwnedBy(PL)
@Getter
@Builder
public class NGVaultRenewalTaskParameters implements TaskParameters, ExecutionCapabilityDemander {
  private final BaseVaultConfig encryptionConfig;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return encryptionConfig.fetchRequiredExecutionCapabilities(maskingEvaluator);
  }
}
