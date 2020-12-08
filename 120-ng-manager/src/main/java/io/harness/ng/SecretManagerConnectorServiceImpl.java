package io.harness.ng;

import static io.harness.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.SRE;
import static io.harness.exception.WingsException.USER;

import io.harness.connector.apis.dto.ConnectorCatalogueResponseDTO;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.connector.apis.dto.ConnectorListFilter;
import io.harness.connector.apis.dto.ConnectorResponseDTO;
import io.harness.connector.apis.dto.stats.ConnectorStatistics;
import io.harness.connector.entities.Connector.ConnectorKeys;
import io.harness.connector.entities.embedded.vaultconnector.VaultConnector.VaultConnectorKeys;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.gcpkmsconnector.GcpKmsConnectorDTO;
import io.harness.delegate.beans.connector.localconnector.LocalConnectorDTO;
import io.harness.delegate.beans.connector.vaultconnector.VaultConnectorDTO;
import io.harness.encryption.Scope;
import io.harness.eraro.ErrorCode;
import io.harness.exception.SecretManagementException;
import io.harness.exception.WingsException;
import io.harness.ng.core.api.NGSecretManagerService;
import io.harness.repositories.ConnectorRepository;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigUpdateDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Optional;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@Slf4j
public class SecretManagerConnectorServiceImpl implements ConnectorService {
  private final ConnectorService defaultConnectorService;
  private final NGSecretManagerService ngSecretManagerService;
  private final ConnectorRepository connectorRepository;

  @Inject
  public SecretManagerConnectorServiceImpl(@Named(DEFAULT_CONNECTOR_SERVICE) ConnectorService defaultConnectorService,
      NGSecretManagerService ngSecretManagerService, ConnectorRepository connectorRepository) {
    this.defaultConnectorService = defaultConnectorService;
    this.ngSecretManagerService = ngSecretManagerService;
    this.connectorRepository = connectorRepository;
  }

  @Override
  public Page<ConnectorResponseDTO> list(
      int page, int size, String accountIdentifier, ConnectorListFilter connectorFilter) {
    throw new UnsupportedOperationException("This operation is not supported for secret manager");
  }

  @Override
  public Optional<ConnectorResponseDTO> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    return defaultConnectorService.get(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
  }

  @Override
  public ConnectorResponseDTO create(@Valid ConnectorDTO connector, String accountIdentifier) {
    // TODO{karan} Remove this section after event driven is used to create harness secret manager for account
    ConnectorInfoDTO connectorInfo = connector.getConnectorInfo();
    if (connectorInfo.getIdentifier().equals(HARNESS_SECRET_MANAGER_IDENTIFIER)) {
      if (!defaultConnectorService.get(accountIdentifier, null, null, HARNESS_SECRET_MANAGER_IDENTIFIER).isPresent()) {
        log.info("Account level Harness Secret Manager not found");
        String orgIdentifier = connectorInfo.getOrgIdentifier();
        String projectIdentifier = connectorInfo.getProjectIdentifier();
        String description = connectorInfo.getDescription();
        String name = connectorInfo.getName();
        connectorInfo.setOrgIdentifier(null);
        connectorInfo.setProjectIdentifier(null);
        connectorInfo.setDescription("Account Level Secret Manager");
        connectorInfo.setName("Harness Secrets Manager");
        createSecretManagerConnector(connector, accountIdentifier);
        connectorInfo.setProjectIdentifier(projectIdentifier);
        connectorInfo.setOrgIdentifier(orgIdentifier);
        connectorInfo.setDescription(description);
        connectorInfo.setName(name);
      }
    }
    return createSecretManagerConnector(connector, accountIdentifier);
  }

  private ConnectorResponseDTO createSecretManagerConnector(ConnectorDTO connector, String accountIdentifier) {
    ConnectorInfoDTO connectorInfo = connector.getConnectorInfo();
    SecretManagerConfigDTO secretManagerConfigDTO =
        SecretManagerConfigDTOMapper.fromConnectorDTO(accountIdentifier, connector, connectorInfo.getConnectorConfig());
    SecretManagerConfigDTO createdSecretManager = ngSecretManagerService.createSecretManager(secretManagerConfigDTO);
    if (Optional.ofNullable(createdSecretManager).isPresent()) {
      try {
        if (isDefaultSecretManager(connector.getConnectorInfo())) {
          setDefaultFlagFalseOfSecretManagers(accountIdentifier, connector.getConnectorInfo().getOrgIdentifier(),
              connector.getConnectorInfo().getProjectIdentifier());
        }
        return defaultConnectorService.create(connector, accountIdentifier);
      } catch (Exception ex) {
        log.error("Error occurred while creating secret manager in 120 ng, trying to delete in 71 rest", ex);
        ngSecretManagerService.deleteSecretManager(accountIdentifier, connectorInfo.getOrgIdentifier(),
            connectorInfo.getProjectIdentifier(), connectorInfo.getIdentifier());
        throw new SecretManagementException(
            SECRET_MANAGEMENT_ERROR, "Exception occurred while saving secret manager", USER);
      }
    }
    throw new SecretManagementException(
        SECRET_MANAGEMENT_ERROR, "Error occurred while saving secret manager in 71 rest.", SRE);
  }

  private boolean isDefaultSecretManager(ConnectorInfoDTO connector) {
    switch (connector.getConnectorType()) {
      case VAULT:
        return ((VaultConnectorDTO) connector.getConnectorConfig()).isDefault();
      case GCP_KMS:
        return ((GcpKmsConnectorDTO) connector.getConnectorConfig()).isDefault();
      case LOCAL:
        return ((LocalConnectorDTO) connector.getConnectorConfig()).isDefault();
      default:
        throw new SecretManagementException(ErrorCode.SECRET_MANAGEMENT_ERROR,
            String.format("Unsupported Secret Manager type [%s]", connector.getConnectorType()), WingsException.USER);
    }
  }

  private void setDefaultFlagFalseOfSecretManagers(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Criteria criteria = Criteria.where(ConnectorKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(ConnectorKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(ConnectorKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(ConnectorKeys.categories)
                            .in(ConnectorCategory.SECRET_MANAGER)
                            .and(ConnectorKeys.deleted)
                            .ne(Boolean.TRUE)
                            .and(VaultConnectorKeys.isDefault)
                            .is(Boolean.TRUE);

    Query query = new Query(criteria);
    Update update = new Update().set(VaultConnectorKeys.isDefault, Boolean.FALSE);
    connectorRepository.updateMultiple(query, update);
  }

  @Override
  public ConnectorResponseDTO update(ConnectorDTO connector, String accountIdentifier) {
    ConnectorInfoDTO connectorInfo = connector.getConnectorInfo();
    SecretManagerConfigUpdateDTO dto =
        SecretManagerConfigUpdateDTOMapper.fromConnectorDTO(connector, connectorInfo.getConnectorConfig());
    SecretManagerConfigDTO updatedSecretManagerConfig = ngSecretManagerService.updateSecretManager(accountIdentifier,
        connectorInfo.getOrgIdentifier(), connectorInfo.getProjectIdentifier(), connectorInfo.getIdentifier(), dto);
    if (Optional.ofNullable(updatedSecretManagerConfig).isPresent()) {
      if (isDefaultSecretManager(connector.getConnectorInfo())) {
        setDefaultFlagFalseOfSecretManagers(accountIdentifier, connector.getConnectorInfo().getOrgIdentifier(),
            connector.getConnectorInfo().getProjectIdentifier());
      }
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
  public ConnectorValidationResult validate(ConnectorDTO connector, String accountIdentifier) {
    throw new UnsupportedOperationException("Cannot validate secret manager, use test connection API instead");
  }

  @Override
  public ConnectorValidationResult testConnection(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    long currentTime = System.currentTimeMillis();
    try {
      boolean success =
          ngSecretManagerService.validate(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
      return ConnectorValidationResult.builder().valid(success).testedAt(currentTime).build();
    } catch (Exception exception) {
      log.info("Test connection for connector {}, {}, {}, {} failed.", accountIdentifier, orgIdentifier,
          projectIdentifier, connectorIdentifier, exception);
      return ConnectorValidationResult.builder()
          .valid(false)
          .errorMessage(exception.getMessage())
          .testedAt(currentTime)
          .build();
    }
  }

  @Override
  public ConnectorCatalogueResponseDTO getConnectorCatalogue() {
    return defaultConnectorService.getConnectorCatalogue();
  }

  @Override
  public void updateConnectorEntityWithPerpetualtaskId(
      String accountIdentifier, ConnectorInfoDTO connector, String perpetualTaskId) {
    defaultConnectorService.updateConnectorEntityWithPerpetualtaskId(accountIdentifier, connector, perpetualTaskId);
  }

  @Override
  public ConnectorValidationResult testGitRepoConnection(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String connectorIdentifier, String gitRepoURL) {
    return defaultConnectorService.testGitRepoConnection(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier, gitRepoURL);
  }

  @Override
  public ConnectorStatistics getConnectorStatistics(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, Scope scope) {
    return defaultConnectorService.getConnectorStatistics(accountIdentifier, orgIdentifier, projectIdentifier, scope);
  }

  @Override
  public Page<ConnectorResponseDTO> list(int page, int size, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, ConnectorType type, ConnectorCategory category) {
    throw new UnsupportedOperationException("Cannot call list api on secret manager");
  }
}
