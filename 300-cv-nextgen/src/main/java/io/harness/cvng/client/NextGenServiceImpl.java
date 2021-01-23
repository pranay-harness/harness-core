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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

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
  public Map<String, ServiceResponseDTO> listServicesForProject(
      String accountId, String orgIdentifier, String projectIdentifier, Set<String> serviceIdentifiers) {
    PageResponse<ServiceResponseDTO> services =
        requestExecutor
            .execute(nextGenClient.listServicesForProject(
                0, serviceIdentifiers.size(), accountId, orgIdentifier, projectIdentifier, serviceIdentifiers))
            .getData();
    return services.getContent().stream().collect(
        Collectors.toMap(ServiceResponseDTO::getIdentifier, Function.identity()));
  }

  @Override
  public Map<String, EnvironmentResponseDTO> listEnvironmentsForProject(@NotNull String accountId,
      @NotNull String orgIdentifier, @NotNull String projectIdentifier, @NotNull Set<String> envIdentifiers) {
    PageResponse<EnvironmentResponseDTO> environments =
        requestExecutor
            .execute(nextGenClient.listEnvironmentsForProject(
                0, envIdentifiers.size(), accountId, orgIdentifier, projectIdentifier, envIdentifiers, null))
            .getData();
    return environments.getContent().stream().collect(
        Collectors.toMap(EnvironmentResponseDTO::getIdentifier, Function.identity()));
  }

  @Override
  public int getServicesCount(String accountId, String orgIdentifier, String projectIdentifier) {
    return (int) requestExecutor
        .execute(nextGenClient.listServicesForProject(0, 1000, accountId, orgIdentifier, projectIdentifier, null))
        .getData()
        .getTotalItems();
  }

  @Override
  public int getEnvironmentCount(String accountId, String orgIdentifier, String projectIdentifier) {
    return (int) requestExecutor
        .execute(
            nextGenClient.listEnvironmentsForProject(0, 1000, accountId, orgIdentifier, projectIdentifier, null, null))
        .getData()
        .getTotalItems();
  }
}
