package io.harness.connector.services;

import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.apis.dto.ConnectorSummaryDTO;
import org.springframework.data.domain.Page;

import java.util.Optional;

interface ConnectorCrudService {
  Page<ConnectorSummaryDTO> list(int page, int size, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, String type);

  Optional<ConnectorDTO> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);

  ConnectorDTO create(ConnectorRequestDTO connector, String accountIdentifier);

  ConnectorDTO update(ConnectorRequestDTO connectorRequestDTO, String accountIdentifier);

  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);
}
