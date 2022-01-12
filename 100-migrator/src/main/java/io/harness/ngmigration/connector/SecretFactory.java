/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.connector;

import io.harness.beans.SecretManagerConfig;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;

import software.wings.beans.GcpKmsConfig;
import software.wings.beans.LocalEncryptionConfig;

public class SecretFactory {
  public static ConnectorType getConnectorType(SecretManagerConfig secretManagerConfig) {
    if (secretManagerConfig instanceof GcpKmsConfig) {
      return ConnectorType.GCP_KMS;
    }
    if (secretManagerConfig instanceof LocalEncryptionConfig) {
      return ConnectorType.LOCAL;
    }
    throw new UnsupportedOperationException("Only Google KMS is supported");
  }

  public static ConnectorConfigDTO getConfigDTO(SecretManagerConfig secretManagerConfig) {
    return null;
  }
}
