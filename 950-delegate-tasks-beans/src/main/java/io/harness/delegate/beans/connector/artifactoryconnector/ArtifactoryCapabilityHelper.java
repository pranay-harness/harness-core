package io.harness.delegate.beans.connector.artifactoryconnector;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import java.util.Collections;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ArtifactoryCapabilityHelper {
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      ExpressionEvaluator maskingEvaluator, ArtifactoryConnectorDTO artifactoryConnectorDTO) {
    final String artifactoryServerUrl = artifactoryConnectorDTO.getArtifactoryServerUrl();
    return Collections.singletonList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        artifactoryServerUrl, maskingEvaluator));
  }
}
