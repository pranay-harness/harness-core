package io.harness.delegate.beans.nexus;

import io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NexusCapabilityHelper extends ConnectorCapabilityBaseHelper {
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      ConnectorConfigDTO connectorConfigDTO, Set<String> delegateSelectors, ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> capabilityList = new ArrayList<>();
    NexusConnectorDTO nexusConnectorDTO = (NexusConnectorDTO) connectorConfigDTO;
    final String nexusServerUrl = nexusConnectorDTO.getNexusServerUrl();
    capabilityList.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        nexusServerUrl.endsWith("/") ? nexusServerUrl : nexusServerUrl.concat("/"), maskingEvaluator));
    populateDelegateSelectorCapability(capabilityList, delegateSelectors);
    return capabilityList;
  }
}
