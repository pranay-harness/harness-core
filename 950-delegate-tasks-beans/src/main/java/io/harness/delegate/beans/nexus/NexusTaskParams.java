/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.nexus;

import io.harness.delegate.beans.connector.ConnectorTaskParams;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class NexusTaskParams extends ConnectorTaskParams implements ExecutionCapabilityDemander, TaskParameters {
  NexusConnectorDTO nexusConnectorDTO;
  List<EncryptedDataDetail> encryptedDataDetails;
  TaskType taskType;

  public enum TaskType { VALIDATE }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return NexusCapabilityHelper.fetchRequiredExecutionCapabilities(
        nexusConnectorDTO, this.getDelegateSelectors(), maskingEvaluator);
  }
}
