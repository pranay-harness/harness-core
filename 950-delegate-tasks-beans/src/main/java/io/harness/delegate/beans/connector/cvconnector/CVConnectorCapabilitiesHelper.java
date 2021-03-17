package io.harness.delegate.beans.connector.cvconnector;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsCapabilityHelper;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.k8Connector.K8sTaskCapabilityHelper;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;
import io.harness.delegate.beans.connector.newrelicconnector.NewRelicCapabilityHelper;
import io.harness.delegate.beans.connector.splunkconnector.SplunkCapabilityHelper;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.ExpressionEvaluator;

import java.util.Arrays;
import java.util.List;

public class CVConnectorCapabilitiesHelper {
  public static List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      ConnectorConfigDTO connectorDTO, ExpressionEvaluator maskingEvaluator) {
    if (connectorDTO instanceof KubernetesClusterConfigDTO) {
      return K8sTaskCapabilityHelper.fetchRequiredExecutionCapabilities(
          (KubernetesClusterConfigDTO) connectorDTO, maskingEvaluator);
    } else if (connectorDTO instanceof AppDynamicsConnectorDTO) {
      return AppDynamicsCapabilityHelper.fetchRequiredExecutionCapabilities(
          maskingEvaluator, (AppDynamicsConnectorDTO) connectorDTO);
    } else if (connectorDTO instanceof SplunkConnectorDTO) {
      return SplunkCapabilityHelper.fetchRequiredExecutionCapabilities(
          maskingEvaluator, (SplunkConnectorDTO) connectorDTO);
    } else if (connectorDTO instanceof GcpConnectorDTO) {
      return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
          "https://storage.cloud.google.com/", maskingEvaluator));
    } else if (connectorDTO instanceof NewRelicConnectorDTO) {
      return NewRelicCapabilityHelper.fetchRequiredExecutionCapabilities(
          maskingEvaluator, (NewRelicConnectorDTO) connectorDTO);
    } else {
      throw new InvalidRequestException("Connector capability not found for " + connectorDTO);
    }
  }
}
