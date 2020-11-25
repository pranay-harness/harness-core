package io.harness.delegate.beans.connector.cvconnector;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.common.base.Preconditions;
import java.util.List;
import lombok.Builder;
import lombok.Value;

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
