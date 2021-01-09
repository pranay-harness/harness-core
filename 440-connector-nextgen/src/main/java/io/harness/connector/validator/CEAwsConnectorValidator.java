package io.harness.connector.validator;

import io.harness.delegate.beans.connector.ConnectivityStatus;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;

import java.time.Instant;

public class CEAwsConnectorValidator implements ConnectionValidator<CEAwsConnectorDTO> {
  @Override
  public ConnectorValidationResult validate(
      CEAwsConnectorDTO connectorDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    // TODO(UTSAV) : implement it after configuring AWS account credentials and validation helper
    // which fetches the bucketRegion and bucketPrefix in DTOToEntity and also validates the config.
    return ConnectorValidationResult.builder()
        .status(ConnectivityStatus.SUCCESS)
        .testedAt(Instant.now().toEpochMilli())
        .build();
  }
}
