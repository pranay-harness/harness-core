package io.harness.delegate.beans.connector.nexusconnector;

import io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NexusCapabilityHelper extends ConnectorCapabilityBaseHelper {
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      ExpressionEvaluator maskingEvaluator, NexusConnectorDTO nexusConnectorDTO) {
    List<ExecutionCapability> capabilityList = new ArrayList<>();
    String nexusServerUrl = nexusConnectorDTO.getNexusServerUrl();
    capabilityList.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        nexusServerUrl, maskingEvaluator));
    populateDelegateSelectorCapability(capabilityList, nexusConnectorDTO.getDelegateSelectors());
    return capabilityList;
  }
}