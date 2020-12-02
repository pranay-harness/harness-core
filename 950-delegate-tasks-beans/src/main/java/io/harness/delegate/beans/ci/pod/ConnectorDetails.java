package io.harness.delegate.beans.ci.pod;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class ConnectorDetails {
  @NotNull ConnectorConfigDTO connectorConfig;
  @NotNull ConnectorType connectorType;
  @NotNull String identifier;
  String orgIdentifier;
  String projectIdentifier;
  @NotNull List<EncryptedDataDetail> encryptedDataDetails;
  @Singular("envToSecretEntry") Map<EnvVariableEnum, String> envToSecretsMap;
}
