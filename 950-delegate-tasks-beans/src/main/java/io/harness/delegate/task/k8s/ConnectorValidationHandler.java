package io.harness.delegate.task.k8s;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;

public abstract class ConnectorValidationHandler {
  public ConnectorValidationResult validate(
      ConnectorConfigDTO connector, String accountIdentifier, List<EncryptedDataDetail> encryptionDetailList) {
    throw new UnsupportedOperationException(
        "The validate method is not supported for the abstract class ConnectorValidationHandler");
  }
}