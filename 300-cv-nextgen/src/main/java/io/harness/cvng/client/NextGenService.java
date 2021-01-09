package io.harness.cvng.client;

import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;

import java.util.List;
import java.util.Optional;

public interface NextGenService {
  ConnectorResponseDTO create(ConnectorDTO connectorRequestDTO, String accountIdentifier);

  Optional<ConnectorInfoDTO> get(
      String accountIdentifier, String connectorIdentifier, String orgIdentifier, String projectIdentifier);

  EnvironmentResponseDTO getEnvironment(
      String environmentIdentifier, String accountId, String orgIdentifier, String projectIdentifier);

  ServiceResponseDTO getService(
      String serviceIdentifier, String accountId, String orgIdentifier, String projectIdentifier);

  int getServicesCount(String accountId, String orgIdentifier, String projectIdentifier);

  PageResponse<ServiceResponseDTO> getServices(
      int page, int size, String accountId, String orgIdentifier, String projectIdentifier, List<String> sort);

  PageResponse<EnvironmentResponseDTO> listEnvironmentsForProject(
      int page, int size, String accountId, String orgIdentifier, String projectIdentifier, List<String> sort);

  int getEnvironmentCount(String accountId, String orgIdentifier, String projectIdentifier);
}
