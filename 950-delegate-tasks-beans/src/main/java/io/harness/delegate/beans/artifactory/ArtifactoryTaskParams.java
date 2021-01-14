package io.harness.delegate.beans.artifactory;

import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ArtifactoryTaskParams implements ExecutionCapabilityDemander, TaskParameters {
  ArtifactoryConnectorDTO artifactoryConnectorDTO;
  TaskType taskType;
  List<EncryptedDataDetail> encryptedDataDetails;

  public enum TaskType { VALIDATE }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    final String artifactoryServerUrl = artifactoryConnectorDTO.getArtifactoryServerUrl();
    return Collections.singletonList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        artifactoryServerUrl.endsWith("/") ? artifactoryServerUrl : artifactoryServerUrl.concat("/"),
        maskingEvaluator));
  }
}
