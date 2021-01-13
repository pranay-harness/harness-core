package io.harness.connector;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.task.k8s.ConnectorValidationHandler;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;

// to be removed once everyone adheres to validator
public class NoOpConnectorValidationHandler extends ConnectorValidationHandler {
  @Override
  public ConnectorValidationResult validate(
      ConnectorConfigDTO connector, String accountIdentifier, List<EncryptedDataDetail> encryptionDetailList) {
    return null;
  }
}
