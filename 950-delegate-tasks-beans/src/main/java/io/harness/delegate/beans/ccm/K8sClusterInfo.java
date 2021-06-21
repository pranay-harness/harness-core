package io.harness.delegate.beans.ccm;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(CE)
public class K8sClusterInfo {
  ConnectorConfigDTO connectorConfigDTO;
  List<EncryptedDataDetail> encryptedDataDetails;
}
