package io.harness.connector.services;

import io.harness.connector.apis.dtos.ConnectorFilter;
import io.harness.connector.apis.dtos.connector.ConnectorDTO;
import io.harness.connector.apis.dtos.connector.ConnectorRequestDTO;
import io.harness.connector.apis.dtos.connector.ConnectorSummaryDTO;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface ConnectorService {
  Optional<ConnectorDTO> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);

  Page<ConnectorSummaryDTO> list(ConnectorFilter connectorFilter, int page, int size);

  ConnectorDTO create(ConnectorRequestDTO connector);

  ConnectorDTO update(ConnectorRequestDTO connectorRequestDTO);

  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);
}
