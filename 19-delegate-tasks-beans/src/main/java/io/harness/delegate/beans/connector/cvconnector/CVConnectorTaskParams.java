package io.harness.delegate.beans.connector.cvconnector;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CVConnectorTaskParams implements TaskParameters {
  ConnectorConfigDTO connectorConfigDTO;
  List<EncryptedDataDetail> encryptionDetails;
}
