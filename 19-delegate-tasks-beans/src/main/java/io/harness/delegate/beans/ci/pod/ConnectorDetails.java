package io.harness.delegate.beans.ci.pod;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import javax.validation.constraints.NotNull;

@Value
@Builder
public class ConnectorDetails {
  @NotNull ConnectorConfigDTO connectorConfig;
  @NotNull ConnectorType connectorType;
  @NotNull String identifier;
  String orgIdentifier;
  String projectIdentifier;
  @NotNull List<EncryptedDataDetail> encryptedDataDetails;
}
