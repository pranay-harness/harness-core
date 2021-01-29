package io.harness.delegate.beans.connector.docker;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import java.util.Collections;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DockerCapabilityHelper {
  public static List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      DockerConnectorDTO dockerConnectorDTO, ExpressionEvaluator maskingEvaluator) {
    final String dockerRegistryUrl = dockerConnectorDTO.getDockerRegistryUrl();
    return Collections.singletonList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        dockerRegistryUrl.endsWith("/") ? dockerRegistryUrl : dockerRegistryUrl.concat("/"), maskingEvaluator));
  }
}
