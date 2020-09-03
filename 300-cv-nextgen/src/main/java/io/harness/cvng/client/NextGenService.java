package io.harness.cvng.client;

import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;

import java.util.Optional;

public interface NextGenService {
  ConnectorDTO create(ConnectorRequestDTO connectorRequestDTO, String accountIdentifier);

  Optional<ConnectorDTO> get(
      String accountIdentifier, String connectorIdentifier, String orgIdentifier, String projectIdentifier);

  EnvironmentResponseDTO getEnvironment(
      String environmentIdentifier, String accountId, String orgIdentifier, String projectIdentifier);

  ServiceResponseDTO getService(
      String serviceIdentifier, String accountId, String orgIdentifier, String projectIdentifier);
}
