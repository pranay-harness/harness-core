package io.harness.delegate.beans.connector.cvconnector;

import com.google.common.base.Preconditions;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CVConnectorTaskParams implements TaskParameters, ExecutionCapabilityDemander {
  ConnectorConfigDTO connectorConfigDTO;
  List<EncryptedDataDetail> encryptionDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    Preconditions.checkState(connectorConfigDTO instanceof ExecutionCapabilityDemander,
        "ConnectorConfigDTO should implement ExecutionCapabilityDemander");
    return ((ExecutionCapabilityDemander) connectorConfigDTO).fetchRequiredExecutionCapabilities();
  }
}
