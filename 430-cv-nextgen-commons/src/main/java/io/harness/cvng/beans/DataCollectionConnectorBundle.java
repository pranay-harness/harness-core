package io.harness.cvng.beans;

import io.harness.connector.ConnectorInfoDTO;
import io.harness.cvng.beans.activity.ActivitySourceDTO;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.K8sTaskCapabilityHelper;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.expression.ExpressionEvaluator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DataCollectionConnectorBundle implements ExecutionCapabilityDemander {
  // JSON serialization does not work for ConnectorConfigDTO without the wrapper so need to pass the whole object
  private ConnectorInfoDTO connectorDTO;
  private Map<String, String> params;
  DataCollectionType dataCollectionType;
  ActivitySourceDTO activitySourceDTO;

  @JsonIgnore
  public ConnectorConfigDTO getConnectorConfigDTO() {
    return connectorDTO.getConnectorConfig();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    // TODO: this is a stop gap fix, we will be refactoring it once DX team works on their proposal
    switch (connectorDTO.getConnectorType()) {
      case KUBERNETES_CLUSTER:
        return K8sTaskCapabilityHelper.fetchRequiredExecutionCapabilities(
            (KubernetesClusterConfigDTO) connectorDTO.getConnectorConfig(), maskingEvaluator);
      default:
        break;
    }
    Preconditions.checkState(getConnectorConfigDTO() instanceof ExecutionCapabilityDemander,
        "ConnectorConfigDTO should impalement ExecutionCapabilityDemander");
    return ((ExecutionCapabilityDemander) getConnectorConfigDTO()).fetchRequiredExecutionCapabilities(maskingEvaluator);
  }
}
