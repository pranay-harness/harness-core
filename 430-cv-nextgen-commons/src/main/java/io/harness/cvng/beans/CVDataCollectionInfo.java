package io.harness.cvng.beans;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class CVDataCollectionInfo {
  private ConnectorConfigDTO connectorConfigDTO;
  private List<EncryptedDataDetail> encryptedDataDetails;
  private DataCollectionType dataCollectionType;
}
