package io.harness.cvng.client;

import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;

import java.util.Optional;

public interface NextGenService {
  ConnectorResponseDTO create(ConnectorDTO connectorRequestDTO, String accountIdentifier);

  Optional<ConnectorInfoDTO> get(
      String accountIdentifier, String connectorIdentifier, String orgIdentifier, String projectIdentifier);

  EnvironmentResponseDTO getEnvironment(
      String accountId, String orgIdentifier, String projectIdentifier, String environmentIdentifier);

  ServiceResponseDTO getService(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier);

  int getServicesCount(String accountId, String orgIdentifier, String projectIdentifier);

  int getEnvironmentCount(String accountId, String orgIdentifier, String projectIdentifier);
}
