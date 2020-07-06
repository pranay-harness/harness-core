package io.harness.connector.services;

import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorFilter;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.apis.dto.ConnectorSummaryDTO;
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
