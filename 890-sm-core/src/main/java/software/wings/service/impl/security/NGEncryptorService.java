/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;

@OwnedBy(PL)
public interface NGEncryptorService {
  void decryptEncryptionConfigSecrets(ConnectorConfigDTO connectorConfigDTO, String accountIdentifier,
      String projectIdentifier, String orgIdentifier, boolean maskSecrets);

  char[] fetchSecretValue(
      String accountIdentifier, EncryptedRecordData encryptedData, EncryptionConfig secretManagerConfig);
}
