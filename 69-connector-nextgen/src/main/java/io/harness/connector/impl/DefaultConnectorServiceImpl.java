package io.harness.connector.impl;

import static io.harness.connector.entities.ConnectivityStatus.FAILURE;
import static io.harness.connector.entities.ConnectivityStatus.SUCCESS;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.connector.ConnectorFilterHelper;
import io.harness.connector.ConnectorScopeHelper;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.apis.dto.ConnectorSummaryDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.Connector.ConnectorKeys;
import io.harness.connector.entities.ConnectorConnectivityDetails;
import io.harness.connector.entities.ConnectorConnectivityDetails.ConnectorConnectivityDetailsBuilder;
import io.harness.connector.mappers.ConnectorMapper;
import io.harness.connector.repositories.base.ConnectorRepository;
import io.harness.connector.services.ConnectorService;
import io.harness.connector.validator.ConnectionValidator;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.utils.FullyQualifiedIdentifierHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class DefaultConnectorServiceImpl implements ConnectorService {
  private final ConnectorMapper connectorMapper;
  private final ConnectorRepository connectorRepository;
  private final ConnectorFilterHelper connectorFilterHelper;
  private ConnectorScopeHelper connectorScopeHelper;
  private Map<String, ConnectionValidator> connectionValidatorMap;
  private SecretManagerClientService secretManagerClientService;

  @Override
  public Optional<ConnectorDTO> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    Optional<Connector> connector = connectorRepository.findByFullyQualifiedIdentifier(fullyQualifiedIdentifier);
    if (connector.isPresent()) {
      return Optional.of(connectorMapper.writeDTO(connector.get()));
    }
    throw new InvalidRequestException(
        createConnectorNotFoundMessage(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
  }

  private String createConnectorNotFoundMessage(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    StringBuilder stringBuilder = new StringBuilder(256);
    stringBuilder.append("No connector exists with the identifier ")
        .append(connectorIdentifier)
        .append(" in account ")
        .append(accountIdentifier);
    if (isNotBlank(orgIdentifier)) {
      stringBuilder.append(", organisation ").append(orgIdentifier);
    }
    if (isNotBlank(projectIdentifier)) {
      stringBuilder.append(", project ").append(projectIdentifier);
    }
    return stringBuilder.toString();
  }

  @Override
  public Page<ConnectorSummaryDTO> list(int page, int size, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, String type) {
    Criteria criteria = connectorFilterHelper.createCriteriaFromConnectorFilter(
        accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, type);
    Pageable pageable = getPageRequest(page, size, Sort.by(Sort.Direction.DESC, ConnectorKeys.createdAt));
    Page<Connector> connectors = connectorRepository.findAll(criteria, pageable);
    return connectorScopeHelper.createConnectorSummaryListForConnectors(connectors);
  }

  private Pageable getPageRequest(int page, int size, Sort sort) {
    return PageRequest.of(page, size, sort);
  }

  @Override
  public ConnectorDTO create(ConnectorRequestDTO connectorRequestDTO, String accountIdentifier) {
    Connector connectorEntity = connectorMapper.toConnector(connectorRequestDTO, accountIdentifier);
    Connector savedConnectorEntity = null;
    try {
      savedConnectorEntity = connectorRepository.save(connectorEntity);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(format("Connector [%s] already exists", connectorEntity.getIdentifier()));
    }
    return connectorMapper.writeDTO(savedConnectorEntity);
  }

  @Override
  public ConnectorDTO update(ConnectorRequestDTO connectorRequestDTO, String accountIdentifier) {
    Objects.requireNonNull(connectorRequestDTO.getIdentifier());
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(accountIdentifier,
        connectorRequestDTO.getOrgIdentifier(), connectorRequestDTO.getProjectIdentifier(),
        connectorRequestDTO.getIdentifier());
    Optional<Connector> existingConnector =
        connectorRepository.findByFullyQualifiedIdentifier(fullyQualifiedIdentifier);
    if (!existingConnector.isPresent()) {
      throw new InvalidRequestException(
          format("No connector exists with the  Identitier %s", connectorRequestDTO.getIdentifier()));
    }
    Connector newConnector = connectorMapper.toConnector(connectorRequestDTO, accountIdentifier);
    newConnector.setId(existingConnector.get().getId());
    newConnector.setVersion(existingConnector.get().getVersion());
    Connector updatedConnector = connectorRepository.save(newConnector);
    return connectorMapper.writeDTO(updatedConnector);
  }

  @Override
  public boolean delete(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    Long connectorsDeleted = connectorRepository.deleteByFullyQualifiedIdentifier(fullyQualifiedIdentifier);
    return connectorsDeleted == 1;
  }

  public ConnectorValidationResult validate(ConnectorRequestDTO connectorDTO, String accountIdentifier) {
    ConnectionValidator connectionValidator = connectionValidatorMap.get(connectorDTO.getConnectorType().toString());
    return connectionValidator.validate(connectorDTO.getConnectorConfig(), accountIdentifier,
        connectorDTO.getOrgIdentifier(), connectorDTO.getProjectIdentifier());
  }

  public boolean validateTheIdentifierIsUnique(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    return !connectorRepository.existsByFullyQualifiedIdentifier(fullyQualifiedIdentifier);
  }

  public ConnectorValidationResult testConnection(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    Optional<Connector> connectorOptional =
        connectorRepository.findByFullyQualifiedIdentifier(fullyQualifiedIdentifier);
    if (connectorOptional.isPresent()) {
      ConnectorDTO connectorDTO = connectorMapper.writeDTO(connectorOptional.get());
      ConnectionValidator connectionValidator = connectionValidatorMap.get(connectorDTO.getConnectorType().toString());
      ConnectorValidationResult validationResult;
      try {
        validationResult = connectionValidator.validate(
            connectorDTO.getConnectorConfig(), accountIdentifier, orgIdentifier, projectIdentifier);
      } catch (Exception ex) {
        logger.info(String.format(
            "Test Connection failed for connector [%s] with error [%s]", fullyQualifiedIdentifier, ex.getMessage()));
        validationResult = ConnectorValidationResult.builder().build();
      }
      long connectivityTestedAt = System.currentTimeMillis();
      validationResult.setTestedAt(connectivityTestedAt);
      updateConnectivityStatusOfConnector(connectorOptional.get(), validationResult, connectivityTestedAt);
      return validationResult;
    } else {
      throw new InvalidRequestException(
          createConnectorNotFoundMessage(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
    }
  }

  private void updateConnectivityStatusOfConnector(
      Connector connector, ConnectorValidationResult connectorValidationResult, long connectivityTestedAt) {
    if (connectorValidationResult != null) {
      ConnectorConnectivityDetailsBuilder connectorConnectivityDetailsBuilder =
          ConnectorConnectivityDetails.builder()
              .status(connectorValidationResult.isValid() ? SUCCESS : FAILURE)
              .errorMessage(connectorValidationResult.getErrorMessage())
              .lastTestedAt(connectivityTestedAt);
      if (connectorValidationResult.isValid()) {
        connectorConnectivityDetailsBuilder.lastConnectedAt(connectivityTestedAt);
      }
      connector.setStatus(connectorConnectivityDetailsBuilder.build());
      connectorRepository.save(connector);
    }
  }
}
