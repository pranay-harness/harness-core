package io.harness.connector.services;

import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface ConnectorCrudService {
  Page<ConnectorResponseDTO> list(int page, int size, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, ConnectorType type, ConnectorCategory category);

  Optional<ConnectorResponseDTO> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);

  ConnectorResponseDTO create(ConnectorDTO connector, String accountIdentifier);

  ConnectorResponseDTO update(ConnectorDTO connectorRequestDTO, String accountIdentifier);

  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);
}
