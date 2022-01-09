/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
