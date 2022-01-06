/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.heartbeat;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.beans.DecryptableEntity;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.helper.EncryptionHelper;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.gcp.GcpValidationParams;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import org.jetbrains.annotations.NotNull;

@Singleton
public class GcpValidationParamsProvider implements ConnectorValidationParamsProvider {
  @Inject EncryptionHelper encryptionHelper;

  @Override
  public ConnectorValidationParams getConnectorValidationParams(@NotNull ConnectorInfoDTO connectorInfoDTO,
      String connectorName, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    final GcpConnectorDTO connectorConfig = (GcpConnectorDTO) connectorInfoDTO.getConnectorConfig();
    final List<DecryptableEntity> decryptableEntityList = connectorConfig.getDecryptableEntities();
    DecryptableEntity decryptableEntity = null;
    if (isNotEmpty(decryptableEntityList)) {
      decryptableEntity = decryptableEntityList.get(0);
    }
    final List<EncryptedDataDetail> encryptionDetail =
        encryptionHelper.getEncryptionDetail(decryptableEntity, accountIdentifier, orgIdentifier, projectIdentifier);
    return GcpValidationParams.builder()
        .gcpConnectorDTO(connectorConfig)
        .connectorName(connectorName)
        .encryptionDetails(encryptionDetail)
        .delegateSelectors(connectorConfig.getDelegateSelectors())
        .build();
  }
}
