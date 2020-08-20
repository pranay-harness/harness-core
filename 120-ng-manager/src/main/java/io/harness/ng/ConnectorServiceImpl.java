package io.harness.ng;

import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.exception.WingsException.USER;
import static io.harness.ng.NextGenModule.SECRET_MANAGER_CONNECTOR_SERVICE;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.apis.dto.ConnectorSummaryDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.exception.InvalidRequestException;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

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
    if (connectorType == ConnectorType.VAULT) {
      return secretManagerConnectorService;
    }
    return defaultConnectorService;
  }

  @Override
  public Optional<ConnectorDTO> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    return defaultConnectorService.get(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
  }

  @Override
  public Page<ConnectorSummaryDTO> list(int page, int size, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, ConnectorType type, List<ConnectorCategory> categories) {
    return defaultConnectorService.list(
        page, size, accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, type, categories);
  }

  @Override
  public ConnectorDTO create(ConnectorRequestDTO connector, String accountIdentifier) {
    return getConnectorService(connector.getConnectorType()).create(connector, accountIdentifier);
  }

  @Override
  public ConnectorDTO update(ConnectorRequestDTO connectorRequestDTO, String accountIdentifier) {
    return getConnectorService(connectorRequestDTO.getConnectorType()).update(connectorRequestDTO, accountIdentifier);
  }

  @Override
  public boolean delete(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    Optional<ConnectorDTO> connectorDTO = get(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    if (connectorDTO.isPresent()) {
      return getConnectorService(connectorDTO.get().getConnectorType())
          .delete(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    }
    throw new InvalidRequestException("No such connector found", USER);
  }

  @Override
  public ConnectorValidationResult validate(ConnectorRequestDTO connector, String accountIdentifier) {
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
    return defaultConnectorService.testConnection(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
  }
}
