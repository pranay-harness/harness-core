package io.harness.delegate.task.k8s;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;

public abstract class ConnectorValidationHandler {
  public abstract ConnectorValidationResult validate(
      ConnectorConfigDTO connector, String accountIdentifier, List<EncryptedDataDetail> encryptionDetailList);
}