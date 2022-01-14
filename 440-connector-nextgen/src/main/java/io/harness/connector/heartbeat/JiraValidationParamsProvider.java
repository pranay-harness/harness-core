package io.harness.connector.heartbeat;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.beans.DecryptableEntity;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.helper.EncryptionHelper;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.beans.connector.jira.JiraValidationParams;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.inject.Inject;
import java.util.List;

public class JiraValidationParamsProvider implements ConnectorValidationParamsProvider {
  @Inject EncryptionHelper encryptionHelper;

  @Override
  public ConnectorValidationParams getConnectorValidationParams(ConnectorInfoDTO connectorInfoDTO, String connectorName,
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    final JiraConnectorDTO connectorConfig = (JiraConnectorDTO) connectorInfoDTO.getConnectorConfig();
    final List<DecryptableEntity> decryptableEntityList = connectorConfig.getDecryptableEntities();
    DecryptableEntity decryptableEntity = null;
    if (isNotEmpty(decryptableEntityList)) {
      decryptableEntity = decryptableEntityList.get(0);
    }
    final List<EncryptedDataDetail> encryptionDetail =
        encryptionHelper.getEncryptionDetail(decryptableEntity, accountIdentifier, orgIdentifier, projectIdentifier);
    return JiraValidationParams.builder()
        .jiraConnectorDTO(connectorConfig)
        .connectorName(connectorName)
        .encryptedDataDetails(encryptionDetail)
        .build();
  }
}
