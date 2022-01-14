package io.harness.connector.utils;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import lombok.experimental.UtilityClass;

@OwnedBy(DX)
@UtilityClass
public class ConnectorUtils {
  public void checkForConnectorValidityOrThrow(ConnectorResponseDTO connector) {
    if (!connector.getEntityValidityDetails().isValid()) {
      throw new InvalidRequestException(
          String.format("Connector for identifier [%s] is invalid. Please fix the connector YAML.",
              connector.getConnector().getIdentifier()),
          WingsException.USER);
    }
  }
}
