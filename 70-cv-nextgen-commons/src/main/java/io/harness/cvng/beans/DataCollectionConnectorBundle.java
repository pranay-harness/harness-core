package io.harness.cvng.beans;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DataCollectionConnectorBundle {
  private ConnectorConfigDTO connectorConfigDTO;
  private List<EncryptedDataDetail> details;
  private Map<String, String> params;
}
