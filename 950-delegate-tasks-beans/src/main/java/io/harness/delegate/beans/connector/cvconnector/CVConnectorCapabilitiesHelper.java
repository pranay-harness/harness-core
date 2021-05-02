package io.harness.delegate.beans.connector.cvconnector;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorTaskParams;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsCapabilityHelper;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.delegate.beans.connector.gcp.GcpCapabilityHelper;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.k8Connector.K8sTaskCapabilityHelper;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;
import io.harness.delegate.beans.connector.newrelicconnector.NewRelicCapabilityHelper;
import io.harness.delegate.beans.connector.prometheusconnector.PrometheusCapabilityHelper;
import io.harness.delegate.beans.connector.prometheusconnector.PrometheusConnectorDTO;
import io.harness.delegate.beans.connector.splunkconnector.SplunkCapabilityHelper;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.ExpressionEvaluator;

import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class CVConnectorCapabilitiesHelper extends ConnectorTaskParams {
  protected CVConnectorCapabilitiesHelper(ConnectorTaskParamsBuilder<?, ?> b) {
    super(b);
  }

  public static List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      ConnectorConfigDTO connectorDTO, ExpressionEvaluator maskingEvaluator) {
    if (connectorDTO instanceof KubernetesClusterConfigDTO) {
      return K8sTaskCapabilityHelper.fetchRequiredExecutionCapabilities(connectorDTO, maskingEvaluator);
    } else if (connectorDTO instanceof AppDynamicsConnectorDTO) {
      return AppDynamicsCapabilityHelper.fetchRequiredExecutionCapabilities(connectorDTO, maskingEvaluator);
    } else if (connectorDTO instanceof SplunkConnectorDTO) {
      return SplunkCapabilityHelper.fetchRequiredExecutionCapabilities(connectorDTO, maskingEvaluator);
    } else if (connectorDTO instanceof GcpConnectorDTO) {
      return GcpCapabilityHelper.fetchRequiredExecutionCapabilities(connectorDTO, maskingEvaluator);
    } else if (connectorDTO instanceof NewRelicConnectorDTO) {
      return NewRelicCapabilityHelper.fetchRequiredExecutionCapabilities(maskingEvaluator, connectorDTO);
    } else if (connectorDTO instanceof PrometheusConnectorDTO) {
      return PrometheusCapabilityHelper.fetchRequiredExecutionCapabilities(maskingEvaluator, connectorDTO);
    } else {
      throw new InvalidRequestException("Connector capability not found for " + connectorDTO);
    }
  }
}
