package io.harness.delegate.beans.nexus;

import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import java.util.Collections;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NexusCapabilityHelper {
  public static List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      NexusConnectorDTO nexusConnectorDTO, ExpressionEvaluator maskingEvaluator) {
    final String nexusServerUrl = nexusConnectorDTO.getNexusServerUrl();
    return Collections.singletonList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        nexusServerUrl.endsWith("/") ? nexusServerUrl : nexusServerUrl.concat("/"), maskingEvaluator));
  }
}
