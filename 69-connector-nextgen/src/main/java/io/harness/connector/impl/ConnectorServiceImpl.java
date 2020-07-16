package io.harness.connector.impl;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.connector.ConnectorFilterHelper;
import io.harness.connector.ConnectorScopeHelper;
import io.harness.connector.FullyQualitifedIdentifierHelper;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorFilter;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.apis.dto.ConnectorSummaryDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.mappers.ConnectorMapper;
import io.harness.connector.mappers.ConnectorSummaryMapper;
import io.harness.connector.repositories.ConnectorRepository;
import io.harness.connector.services.ConnectorService;
import io.harness.connector.validator.ConnectionValidator;
import io.harness.connector.validator.KubernetesConnectionValidator;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class ConnectorServiceImpl implements ConnectorService {
  private final ConnectorMapper connectorMapper;
  private final ConnectorRepository connectorRepository;
  private final ConnectorFilterHelper connectorFilterHelper;
  private final ConnectorScopeHelper connectorScopeHelper;
  private final ConnectorSummaryMapper connectorSummaryMapper;
  @Inject private Map<String, ConnectionValidator> connectionValidatorMap;
  private final KubernetesConnectionValidator kubernetesConnectionValidator;

  @Override
  public Optional<ConnectorDTO> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualitifedIdentifierHelper.getFullyQualifiedIdentifier(
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
  public Page<ConnectorSummaryDTO> list(ConnectorFilter connectorFilter, int page, int size, String accountIdentifier) {
    Criteria criteria = connectorFilterHelper.createCriteriaFromConnectorFilter(connectorFilter, accountIdentifier);
    Pageable pageable = getPageRequest(page, size);
    Page<Connector> connectors = connectorRepository.findAll(criteria, pageable);
    return connectorScopeHelper.createConnectorSummaryListForConnectors(connectors);
  }

  private Pageable getPageRequest(int page, int size) {
    return PageRequest.of(page, size);
  }

  @Override
  public ConnectorDTO create(ConnectorRequestDTO connectorRequestDTO, String accountIdentifier) {
    Connector connectorEntity = connectorMapper.toConnector(connectorRequestDTO, accountIdentifier);
    Connector savedConnectorEntity = null;
    try {
      savedConnectorEntity = connectorRepository.save(connectorEntity);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format("Connector [%s] already exists", connectorEntity.getIdentifier()));
    }
    return connectorMapper.writeDTO(savedConnectorEntity);
  }

  @Override
  public ConnectorDTO update(ConnectorRequestDTO connectorRequestDTO, String accountIdentifier) {
    Objects.requireNonNull(connectorRequestDTO.getIdentifier());
    String fullyQualifiedIdentifier = FullyQualitifedIdentifierHelper.getFullyQualifiedIdentifier(accountIdentifier,
        connectorRequestDTO.getOrgIdentifier(), connectorRequestDTO.getProjectIdentifer(),
        connectorRequestDTO.getIdentifier());
    Optional<Connector> existingConnector =
        connectorRepository.findByFullyQualifiedIdentifier(fullyQualifiedIdentifier);
    if (!existingConnector.isPresent()) {
      throw new InvalidRequestException(
          String.format("No connector exists with the  Identitier %s", connectorRequestDTO.getIdentifier()));
    }
    Connector updatedConnectorEntity =
        connectorRepository.save(applyUpdateToConnector(existingConnector.get(), connectorRequestDTO));
    return connectorMapper.writeDTO(updatedConnectorEntity);
  }

  @Override
  public boolean delete(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualitifedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    connectorRepository.deleteByFullyQualifiedIdentifier(fullyQualifiedIdentifier);
    return true;
  }

  @SneakyThrows
  static Connector applyUpdateToConnector(Connector connector, ConnectorRequestDTO updateConnector) {
    String jsonString = new ObjectMapper().writer().writeValueAsString(updateConnector);
    return new ObjectMapper().readerForUpdating(connector).readValue(jsonString);
  }

  public ConnectorValidationResult validate(ConnectorRequestDTO connectorDTO, String accountId) {
    ConnectionValidator connectionValidator = connectionValidatorMap.get(connectorDTO.getConnectorType().toString());
    return connectionValidator.validate(connectorDTO.getConnectorConfig(), accountId);
  }

  public ConnectorValidationResult testConnection(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualitifedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    Optional<Connector> connectorOptional =
        connectorRepository.findByFullyQualifiedIdentifier(fullyQualifiedIdentifier);
    if (connectorOptional.isPresent()) {
      ConnectorDTO connectorDTO = connectorMapper.writeDTO(connectorOptional.get());
      ConnectionValidator connectionValidator = connectionValidatorMap.get(connectorDTO.getConnectorType().toString());
      return connectionValidator.validate(connectorDTO.getConnectorConfig(), accountIdentifier);
    } else {
      throw new InvalidRequestException(
          createConnectorNotFoundMessage(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
    }
  }
}
