/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.artifactory;

import io.harness.delegate.beans.connector.ConnectorTaskParams;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryCapabilityHelper;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Value;
import lombok.experimental.SuperBuilder;

@Value
@SuperBuilder
public class ArtifactoryTaskParams extends ConnectorTaskParams implements ExecutionCapabilityDemander, TaskParameters {
  ArtifactoryConnectorDTO artifactoryConnectorDTO;
  TaskType taskType;
  List<EncryptedDataDetail> encryptedDataDetails;

  public enum TaskType { VALIDATE }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return ArtifactoryCapabilityHelper.fetchRequiredExecutionCapabilities(artifactoryConnectorDTO, maskingEvaluator);
  }
}
