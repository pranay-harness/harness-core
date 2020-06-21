package io.harness.connector.impl;

import com.google.inject.Inject;

import io.harness.connector.apis.dtos.connector.ConnectorDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.mappers.ConnectorMapper;
import io.harness.connector.repositories.ConnectorRepository;
import io.harness.connector.services.ConnectorService;
import io.harness.exception.DuplicateFieldException;
import org.springframework.dao.DuplicateKeyException;

public class ConnectorServiceImpl implements ConnectorService {
  @Inject ConnectorMapper connectorMapper;
  @Inject ConnectorRepository connectorRepository;

  public ConnectorDTO create(ConnectorDTO connectorDTO) {
    Connector connectorEntity = connectorMapper.toConnector(connectorDTO);
    Connector savedConnectorEntity = null;
    try {
      savedConnectorEntity = connectorRepository.save(connectorEntity);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format("Connector [%s] already exists", connectorEntity.getIdentifier()));
    }
    return connectorMapper.writeDTO(savedConnectorEntity);
  }
}
