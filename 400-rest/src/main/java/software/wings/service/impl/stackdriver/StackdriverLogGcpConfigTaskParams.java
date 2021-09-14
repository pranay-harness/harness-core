/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.stackdriver;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.GcpConfig;

import java.util.List;
import lombok.Builder;
import lombok.Value;
@Value
@Builder
public class StackdriverLogGcpConfigTaskParams implements TaskParameters, ExecutionCapabilityDemander {
  private GcpConfig gcpConfig;
  private List<EncryptedDataDetail> encryptedDataDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return StackdriverUtils.fetchRequiredExecutionCapabilitiesForLogs(encryptedDataDetails, maskingEvaluator);
  }
}
