package io.harness.ng;

import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.SRE;
import static io.harness.exception.WingsException.USER;
import static io.harness.secretmanagerclient.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;
import static io.harness.secretmanagerclient.utils.RestClientUtils.getResponse;

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
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigUpdateDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import software.wings.service.impl.security.SecretManagementException;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;

@Singleton
@Slf4j
public class SecretManagerConnectorServiceImpl implements ConnectorService {
  private final ConnectorService defaultConnectorService;
  private final SecretManagerClient secretManagerClient;

  @Inject
  public SecretManagerConnectorServiceImpl(@Named(DEFAULT_CONNECTOR_SERVICE) ConnectorService defaultConnectorService,
      SecretManagerClient secretManagerClient) {
    this.defaultConnectorService = defaultConnectorService;
    this.secretManagerClient = secretManagerClient;
  }

  @Override
  public Page<ConnectorSummaryDTO> list(int page, int size, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, ConnectorType type, List<ConnectorCategory> categories) {
    throw new UnsupportedOperationException("This operation is not supported for secret manager");
  }

  @Override
  public Optional<ConnectorDTO> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    throw new UnsupportedOperationException("This operation is not supported for secret manager");
  }

  @Override
  public ConnectorDTO create(@Valid ConnectorRequestDTO connector, String accountIdentifier) {
    // TODO{karan} Remove this section after event driven is used to create harness secret manager for account
    if (connector.getIdentifier().equals(HARNESS_SECRET_MANAGER_IDENTIFIER)) {
      if (!defaultConnectorService.get(accountIdentifier, null, null, HARNESS_SECRET_MANAGER_IDENTIFIER).isPresent()) {
        logger.info("Account level Harness Secret Manager not found");
        String orgIdentifier = connector.getOrgIdentifier();
        String projectIdentifier = connector.getProjectIdentifier();
        connector.setOrgIdentifier(null);
        connector.setProjectIdentifier(null);
        createSecretManagerConnector(connector, accountIdentifier);
        connector.setProjectIdentifier(projectIdentifier);
        connector.setOrgIdentifier(orgIdentifier);
      }
    }
    return createSecretManagerConnector(connector, accountIdentifier);
  }

  private ConnectorDTO createSecretManagerConnector(ConnectorRequestDTO connector, String accountIdentifier) {
    SecretManagerConfigDTO secretManagerConfigDTO =
        SecretManagerConfigDTOMapper.fromConnectorDTO(accountIdentifier, connector, connector.getConnectorConfig());
    SecretManagerConfigDTO createdSecretManager =
        getResponse(secretManagerClient.createSecretManager(secretManagerConfigDTO));
    if (Optional.ofNullable(createdSecretManager).isPresent()) {
      try {
        return defaultConnectorService.create(connector, accountIdentifier);
      } catch (Exception ex) {
        logger.error("Error occurred while creating secret manager in 71 rest", ex);
        secretManagerClient.deleteSecretManager(connector.getIdentifier(), accountIdentifier,
            connector.getOrgIdentifier(), connector.getProjectIdentifier());
        throw new SecretManagementException(
            SECRET_MANAGEMENT_ERROR, "Exception occurred while saving secret manager", USER);
      }
    }
    throw new SecretManagementException(
        SECRET_MANAGEMENT_ERROR, "Error occurred while saving secret manager in 71 rest manager", SRE);
  }

  @Override
  public ConnectorDTO update(ConnectorRequestDTO connector, String accountIdentifier) {
    SecretManagerConfigUpdateDTO dto =
        SecretManagerConfigUpdateDTOMapper.fromConnectorDTO(connector, connector.getConnectorConfig());
    SecretManagerConfigDTO updatedSecretManagerConfig =
        getResponse(secretManagerClient.updateSecretManager(connector.getIdentifier(), accountIdentifier,
            connector.getOrgIdentifier(), connector.getProjectIdentifier(), dto));
    if (Optional.ofNullable(updatedSecretManagerConfig).isPresent()) {
      return defaultConnectorService.update(connector, accountIdentifier);
    }
    throw new SecretManagementException(
        SECRET_MANAGEMENT_ERROR, "Error occurred while updating secret manager in 71 rest manager", SRE);
  }

  @Override
  public boolean delete(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    boolean success = getResponse(secretManagerClient.deleteSecretManager(
        connectorIdentifier, accountIdentifier, orgIdentifier, projectIdentifier));
    if (success) {
      return defaultConnectorService.delete(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    }
    return false;
  }

  @Override
  public boolean validateTheIdentifierIsUnique(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    throw new UnsupportedOperationException("This operation is not supported for secret manager");
  }

  @Override
  public ConnectorValidationResult validate(ConnectorRequestDTO connector, String accountIdentifier) {
    throw new UnsupportedOperationException("This operation is not supported for secret manager");
  }

  @Override
  public ConnectorValidationResult testConnection(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    throw new UnsupportedOperationException("This operation is not supported for secret manager");
  }
}
