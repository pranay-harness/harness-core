package io.harness.connector.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.connector.ConnectorFilterHelper;
import io.harness.connector.FullyQualitifedIdentifierHelper;
import io.harness.connector.apis.dtos.ConnectorFilter;
import io.harness.connector.apis.dtos.connector.ConnectorDTO;
import io.harness.connector.apis.dtos.connector.ConnectorRequestDTO;
import io.harness.connector.apis.dtos.connector.ConnectorSummaryDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.mappers.ConnectorMapper;
import io.harness.connector.mappers.ConnectorSummaryMapper;
import io.harness.connector.repositories.ConnectorRepository;
import io.harness.connector.services.ConnectorService;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class ConnectorServiceImpl implements ConnectorService {
  private final ConnectorMapper connectorMapper;
  private final ConnectorRepository connectorRepository;
  // todo @deepak move this mongoTemplate to custom repository
  private final MongoTemplate mongoTemplate;
  private final ConnectorFilterHelper connectorFilterHelper;
  private final ConnectorSummaryMapper connectorSummaryMapper;

  @Override
  public Optional<ConnectorDTO> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualitifedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    Optional<Connector> connector = connectorRepository.findByFullyQualifiedIdentifier(fullyQualifiedIdentifier);
    if (connector.isPresent()) {
      return Optional.of(connectorMapper.writeDTO(connector.get()));
    }
    return Optional.empty();
  }

  @Override
  public Page<ConnectorSummaryDTO> list(ConnectorFilter connectorFilter, int page, int size) {
    Criteria criteria = connectorFilterHelper.createCriteriaFromConnectorFilter(connectorFilter);
    Pageable pageable = getPageRequest(page, size);
    Query query = new Query(criteria).with(pageable);
    List<Connector> connectors = mongoTemplate.find(query, Connector.class);
    Page<Connector> connectorsList = PageableExecutionUtils.getPage(
        connectors, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Connector.class));
    return connectorsList.map(connector -> connectorSummaryMapper.writeConnectorSummaryDTO(connector));
  }

  private Pageable getPageRequest(int page, int size) {
    return PageRequest.of(page, size);
  }

  @Override
  public ConnectorDTO create(ConnectorRequestDTO connectorRequestDTO) {
    Connector connectorEntity = connectorMapper.toConnector(connectorRequestDTO);
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
  public ConnectorDTO update(ConnectorRequestDTO connectorRequestDTO) {
    Objects.requireNonNull(connectorRequestDTO.getIdentifier());
    String fullyQualifiedIdentifier = FullyQualitifedIdentifierHelper.getFullyQualifiedIdentifier(
        connectorRequestDTO.getAccountIdentifier(), connectorRequestDTO.getOrgIdentifier(),
        connectorRequestDTO.getProjectIdentifer(), connectorRequestDTO.getIdentifier());
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
}
