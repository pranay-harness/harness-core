package io.harness.ng;

import static io.harness.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.SRE;
import static io.harness.exception.WingsException.USER;

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
import io.harness.ng.core.api.NGSecretManagerService;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigUpdateDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import software.wings.service.impl.security.SecretManagementException;

import java.util.Optional;
import javax.validation.Valid;

@Singleton
@Slf4j
public class SecretManagerConnectorServiceImpl implements ConnectorService {
  private final ConnectorService defaultConnectorService;
  private final NGSecretManagerService ngSecretManagerService;

  @Inject
  public SecretManagerConnectorServiceImpl(@Named(DEFAULT_CONNECTOR_SERVICE) ConnectorService defaultConnectorService,
      NGSecretManagerService ngSecretManagerService) {
    this.defaultConnectorService = defaultConnectorService;
    this.ngSecretManagerService = ngSecretManagerService;
  }

  @Override
  public Page<ConnectorSummaryDTO> list(int page, int size, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, ConnectorType type, ConnectorCategory categories) {
    throw new UnsupportedOperationException("This operation is not supported for secret manager");
  }

  @Override
  public Optional<ConnectorDTO> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    return defaultConnectorService.get(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
  }

  @Override
  public ConnectorDTO create(@Valid ConnectorRequestDTO connector, String accountIdentifier) {
    // TODO{karan} Remove this section after event driven is used to create harness secret manager for account
    if (connector.getIdentifier().equals(HARNESS_SECRET_MANAGER_IDENTIFIER)) {
      if (!defaultConnectorService.get(accountIdentifier, null, null, HARNESS_SECRET_MANAGER_IDENTIFIER).isPresent()) {
        logger.info("Account level Harness Secret Manager not found");
        String orgIdentifier = connector.getOrgIdentifier();
        String projectIdentifier = connector.getProjectIdentifier();
        String description = connector.getDescription();
        String name = connector.getName();
        connector.setOrgIdentifier(null);
        connector.setProjectIdentifier(null);
        connector.setDescription("Account Level Secret Manager");
        connector.setName("Harness Secrets Manager");
        createSecretManagerConnector(connector, accountIdentifier);
        connector.setProjectIdentifier(projectIdentifier);
        connector.setOrgIdentifier(orgIdentifier);
        connector.setDescription(description);
        connector.setName(name);
      }
    }
    return createSecretManagerConnector(connector, accountIdentifier);
  }

  private ConnectorDTO createSecretManagerConnector(ConnectorRequestDTO connector, String accountIdentifier) {
    SecretManagerConfigDTO secretManagerConfigDTO =
        SecretManagerConfigDTOMapper.fromConnectorDTO(accountIdentifier, connector, connector.getConnectorConfig());
    SecretManagerConfigDTO createdSecretManager = ngSecretManagerService.createSecretManager(secretManagerConfigDTO);
    if (Optional.ofNullable(createdSecretManager).isPresent()) {
      try {
        return defaultConnectorService.create(connector, accountIdentifier);
      } catch (Exception ex) {
        logger.error("Error occurred while creating secret manager in 120 ng, trying to delete in 71 rest", ex);
        ngSecretManagerService.deleteSecretManager(accountIdentifier, connector.getOrgIdentifier(),
            connector.getProjectIdentifier(), connector.getIdentifier());
        throw new SecretManagementException(
            SECRET_MANAGEMENT_ERROR, "Exception occurred while saving secret manager", USER);
      }
    }
    throw new SecretManagementException(
        SECRET_MANAGEMENT_ERROR, "Error occurred while saving secret manager in 71 rest.", SRE);
  }

  @Override
  public ConnectorDTO update(ConnectorRequestDTO connector, String accountIdentifier) {
    SecretManagerConfigUpdateDTO dto =
        SecretManagerConfigUpdateDTOMapper.fromConnectorDTO(connector, connector.getConnectorConfig());
    SecretManagerConfigDTO updatedSecretManagerConfig = ngSecretManagerService.updateSecretManager(accountIdentifier,
        connector.getOrgIdentifier(), connector.getProjectIdentifier(), connector.getIdentifier(), dto);
    if (Optional.ofNullable(updatedSecretManagerConfig).isPresent()) {
      return defaultConnectorService.update(connector, accountIdentifier);
    }
    throw new SecretManagementException(
        SECRET_MANAGEMENT_ERROR, "Error occurred while updating secret manager in 71 rest.", SRE);
  }

  @Override
  public boolean delete(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    boolean success = ngSecretManagerService.deleteSecretManager(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    if (success) {
      return defaultConnectorService.delete(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    }
    return false;
  }

  @Override
  public boolean validateTheIdentifierIsUnique(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    return defaultConnectorService.validateTheIdentifierIsUnique(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
  }

  @Override
  public ConnectorValidationResult validate(ConnectorRequestDTO connector, String accountIdentifier) {
    throw new UnsupportedOperationException("Cannot validate secret manager, use test connection API instead");
  }

  @Override
  public ConnectorValidationResult testConnection(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    boolean success =
        ngSecretManagerService.validate(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    return ConnectorValidationResult.builder().valid(success).build();
  }
}