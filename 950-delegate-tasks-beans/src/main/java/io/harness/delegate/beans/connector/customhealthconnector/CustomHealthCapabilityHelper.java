package io.harness.delegate.beans.connector.customhealthconnector;

import static io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper.populateDelegateSelectorCapability;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.CV)
public class CustomHealthCapabilityHelper {
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      ExpressionEvaluator maskingEvaluator, ConnectorConfigDTO customHealthConnectorDTO) {
    List<ExecutionCapability> capabilityList = new ArrayList<>();
    CustomHealthConnectorDTO connectorDTO = (CustomHealthConnectorDTO) customHealthConnectorDTO;
    capabilityList.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        connectorDTO.getBaseURL(), maskingEvaluator));
    populateDelegateSelectorCapability(capabilityList, connectorDTO.getDelegateSelectors());
    return capabilityList;
  }
}
