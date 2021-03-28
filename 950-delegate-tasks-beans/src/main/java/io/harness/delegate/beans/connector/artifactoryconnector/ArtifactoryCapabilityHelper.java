package io.harness.delegate.beans.connector.artifactoryconnector;

import io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ArtifactoryCapabilityHelper extends ConnectorCapabilityBaseHelper {
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      ConnectorConfigDTO connectorConfigDTO, ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> capabilityList = new ArrayList<>();
    ArtifactoryConnectorDTO artifactoryConnectorDTO = (ArtifactoryConnectorDTO) connectorConfigDTO;
    final String artifactoryServerUrl = artifactoryConnectorDTO.getArtifactoryServerUrl();
    capabilityList.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        artifactoryServerUrl.endsWith("/") ? artifactoryServerUrl : artifactoryServerUrl.concat("/"),
        maskingEvaluator));
    populateDelegateSelectorCapability(capabilityList, artifactoryConnectorDTO.getDelegateSelectors());
    return capabilityList;
  }
}
