package io.harness.ng;

import static io.harness.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.exception.WingsException.USER;
import static io.harness.ng.NextGenModule.SECRET_MANAGER_CONNECTOR_SERVICE;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.connector.apis.dto.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import org.springframework.data.domain.Page;
import software.wings.service.impl.security.SecretManagementException;

import java.util.Optional;
import javax.validation.constraints.NotNull;

@Singleton
public class ConnectorServiceImpl implements ConnectorService {
  private final ConnectorService defaultConnectorService;
  private final ConnectorService secretManagerConnectorService;

  @Inject
  public ConnectorServiceImpl(@Named(DEFAULT_CONNECTOR_SERVICE) ConnectorService defaultConnectorService,
      @Named(SECRET_MANAGER_CONNECTOR_SERVICE) ConnectorService secretManagerConnectorService) {
    this.defaultConnectorService = defaultConnectorService;
    this.secretManagerConnectorService = secretManagerConnectorService;
  }

  private ConnectorService getConnectorService(ConnectorType connectorType) {
    if (connectorType == ConnectorType.LOCAL) {
      throw new SecretManagementException(
          ErrorCode.SECRET_MANAGEMENT_ERROR, "Operation not allowed for Secret Manager", USER);
    } else if (connectorType == ConnectorType.VAULT || connectorType == ConnectorType.GCP_KMS) {
      return secretManagerConnectorService;
    }
    return defaultConnectorService;
  }

  @Override
  public Optional<ConnectorResponseDTO> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    return defaultConnectorService.get(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
  }

  @Override
  public Page<ConnectorResponseDTO> list(int page, int size, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, ConnectorType type, ConnectorCategory category) {
    return defaultConnectorService.list(
        page, size, accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, type, category);
  }

  @Override
  public ConnectorResponseDTO create(@NotNull ConnectorDTO connector, String accountIdentifier) {
    ConnectorInfoDTO connectorInfo = connector.getConnectorInfo();
    if (HARNESS_SECRET_MANAGER_IDENTIFIER.equals(connectorInfo.getIdentifier())) {
      throw new InvalidRequestException(
          String.format("%s cannot be used as connector identifier", HARNESS_SECRET_MANAGER_IDENTIFIER), USER);
    }
    return getConnectorService(connectorInfo.getConnectorType()).create(connector, accountIdentifier);
  }

  @Override
  public ConnectorResponseDTO update(@NotNull ConnectorDTO connector, String accountIdentifier) {
    ConnectorInfoDTO connectorInfo = connector.getConnectorInfo();
    if (HARNESS_SECRET_MANAGER_IDENTIFIER.equals(connectorInfo.getIdentifier())) {
      throw new InvalidRequestException("Update operation not supported for Harness Secret Manager", USER);
    }
    return getConnectorService(connectorInfo.getConnectorType()).update(connector, accountIdentifier);
  }

  @Override
  public boolean delete(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    if (HARNESS_SECRET_MANAGER_IDENTIFIER.equals(connectorIdentifier)) {
      throw new InvalidRequestException("Delete operation not supported for Harness Secret Manager", USER);
    }
    Optional<ConnectorResponseDTO> connectorDTO =
        get(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    if (connectorDTO.isPresent()) {
      ConnectorResponseDTO connectorResponse = connectorDTO.get();
      ConnectorInfoDTO connectorInfoDTO = connectorResponse.getConnector();
      return getConnectorService(connectorInfoDTO.getConnectorType())
          .delete(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    }
    throw new InvalidRequestException("No such connector found", USER);
  }

  @Override
  public ConnectorValidationResult validate(@NotNull ConnectorDTO connector, String accountIdentifier) {
    return defaultConnectorService.validate(connector, accountIdentifier);
  }

  @Override
  public boolean validateTheIdentifierIsUnique(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    return defaultConnectorService.validateTheIdentifierIsUnique(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
  }

  @Override
  public ConnectorValidationResult testConnection(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    Optional<ConnectorResponseDTO> connectorDTO =
        get(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    if (connectorDTO.isPresent()) {
      ConnectorResponseDTO connectorResponse = connectorDTO.get();
      ConnectorInfoDTO connectorInfoDTO = connectorResponse.getConnector();
      return getConnectorService(connectorInfoDTO.getConnectorType())
          .testConnection(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    }
    throw new InvalidRequestException("No such connector found", USER);
  }
}
