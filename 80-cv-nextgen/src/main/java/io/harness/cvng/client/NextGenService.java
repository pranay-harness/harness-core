package io.harness.cvng.client;

import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestDTO;

import java.util.Optional;

public interface NextGenService {
  ConnectorDTO create(ConnectorRequestDTO connectorRequestDTO, String accountIdentifier);

  Optional<ConnectorDTO> get(String accountIdentifier, String connectorIdentifier);
}
