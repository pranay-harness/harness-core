/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.ci.pod;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;
import java.util.Set;
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
  Set<String> delegateSelectors;
  @NotNull List<EncryptedDataDetail> encryptedDataDetails;
  SSHKeyDetails sshKeyDetails;
  @Singular("envToSecretEntry") Map<EnvVariableEnum, String> envToSecretsMap;
}
