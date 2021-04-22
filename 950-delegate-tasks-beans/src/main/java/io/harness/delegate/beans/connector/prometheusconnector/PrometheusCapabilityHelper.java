package io.harness.delegate.beans.connector.prometheusconnector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

import static io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper.populateDelegateSelectorCapability;

@UtilityClass
@OwnedBy(HarnessTeam.CV)
public class PrometheusCapabilityHelper {
    public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
            ExpressionEvaluator maskingEvaluator, ConnectorConfigDTO prometheusConnectorDTO) {
        List<ExecutionCapability> capabilityList = new ArrayList<>();
        PrometheusConnectorDTO connectorDTO = (PrometheusConnectorDTO) prometheusConnectorDTO;
        capabilityList.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
                connectorDTO.getUrl(), maskingEvaluator));
        populateDelegateSelectorCapability(capabilityList, connectorDTO.getDelegateSelectors());
        return capabilityList;
    }
}
