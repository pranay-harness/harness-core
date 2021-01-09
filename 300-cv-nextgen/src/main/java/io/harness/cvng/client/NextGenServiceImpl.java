package io.harness.cvng.client;

import io.harness.beans.IdentifierRef;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;

public class NextGenServiceImpl implements NextGenService {
  @Inject NextGenClient nextGenClient;
  @Inject RequestExecutor requestExecutor;

  @Override
  public ConnectorResponseDTO create(ConnectorDTO connectorRequestDTO, String accountIdentifier) {
    return requestExecutor.execute(nextGenClient.create(connectorRequestDTO, accountIdentifier)).getData();
  }

  @Override
  public Optional<ConnectorInfoDTO> get(
      String accountIdentifier, String connectorIdentifier, String orgIdentifier, String projectIdentifier) {
    IdentifierRef identifierRef =
        IdentifierRefHelper.getIdentifierRef(connectorIdentifier, accountIdentifier, orgIdentifier, projectIdentifier);
    ConnectorResponseDTO connectorResponse =
        requestExecutor
            .execute(nextGenClient.get(identifierRef.getIdentifier(), identifierRef.getAccountIdentifier(),
                identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier()))
            .getData();
    return connectorResponse != null ? Optional.of(connectorResponse.getConnector()) : Optional.empty();
  }

  @Override
  public EnvironmentResponseDTO getEnvironment(
      String environmentIdentifier, String accountId, String orgIdentifier, String projectIdentifier) {
    return requestExecutor
        .execute(nextGenClient.getEnvironment(environmentIdentifier, accountId, orgIdentifier, projectIdentifier))
        .getData();
  }

  @Override
  public ServiceResponseDTO getService(
      String serviceIdentifier, String accountId, String orgIdentifier, String projectIdentifier) {
    return requestExecutor
        .execute(nextGenClient.getService(serviceIdentifier, accountId, orgIdentifier, projectIdentifier))
        .getData();
  }

  @Override
  public PageResponse<ServiceResponseDTO> getServices(
      int page, int size, String accountId, String orgIdentifier, String projectIdentifier, List<String> sort) {
    return requestExecutor
        .execute(nextGenClient.listServicesForProject(page, size, accountId, orgIdentifier, projectIdentifier, sort))
        .getData();
  }

  @Override
  public PageResponse<EnvironmentResponseDTO> listEnvironmentsForProject(
      int page, int size, String accountId, String orgIdentifier, String projectIdentifier, List<String> sort) {
    return requestExecutor
        .execute(
            nextGenClient.listEnvironmentsForProject(page, size, accountId, orgIdentifier, projectIdentifier, sort))
        .getData();
  }

  @Override
  public int getServicesCount(String accountId, String orgIdentifier, String projectIdentifier) {
    PageResponse<ServiceResponseDTO> services = getServices(0, 1000, accountId, orgIdentifier, projectIdentifier, null);
    return (int) services.getTotalItems();
  }

  @Override
  public int getEnvironmentCount(String accountId, String orgIdentifier, String projectIdentifier) {
    PageResponse<EnvironmentResponseDTO> environmentResponseDTOS =
        listEnvironmentsForProject(0, 1000, accountId, orgIdentifier, projectIdentifier, null);
    return (int) environmentResponseDTOS.getTotalItems();
  }
}
