package io.harness.connector.validator;

import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;

public class BitbucketConnectorValidator implements ConnectionValidator<BitbucketConnectorDTO> {
  @Override
  public ConnectorValidationResult validate(
      BitbucketConnectorDTO connectorDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    // Setting always true until we implement.
    return ConnectorValidationResult.builder().valid(true).testedAt(System.currentTimeMillis()).build();
  }
}
