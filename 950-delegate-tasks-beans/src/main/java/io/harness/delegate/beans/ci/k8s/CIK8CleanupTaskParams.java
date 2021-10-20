package io.harness.delegate.beans.ci.k8s;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.delegate.beans.ci.CICleanupTaskParams;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.connector.k8Connector.K8sTaskCapabilityHelper;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.expression.Expression;
import io.harness.expression.ExpressionEvaluator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CIK8CleanupTaskParams implements CICleanupTaskParams, ExecutionCapabilityDemander {
  @NotNull private ConnectorDetails k8sConnector;
  @NotNull private List<String> podNameList; // Currently only single pod deletion is supported for each stage
  @NotNull private List<String> serviceNameList;
  @NotNull private List<String> cleanupContainerNames;
  @Expression(ALLOW_SECRETS) @NotNull private String namespace;
  @Builder.Default private static final CICleanupTaskParams.Type type = Type.GCP_K8;

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    KubernetesClusterConfigDTO kubernetesClusterConfigDTO =
        (KubernetesClusterConfigDTO) k8sConnector.getConnectorConfig();
    return K8sTaskCapabilityHelper.fetchRequiredExecutionCapabilities(kubernetesClusterConfigDTO, maskingEvaluator);
  }
}
