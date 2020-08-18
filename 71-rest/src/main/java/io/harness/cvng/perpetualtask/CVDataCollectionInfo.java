package io.harness.cvng.perpetualtask;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CVDataCollectionInfo {
  ConnectorConfigDTO connectorConfigDTO;
  List<EncryptedDataDetail> encryptedDataDetails;
}
