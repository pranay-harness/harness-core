package io.harness.connector.services;

import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorFilter;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.apis.dto.ConnectorSummaryDTO;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface ConnectorService {
  Optional<ConnectorDTO> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);

  Page<ConnectorSummaryDTO> list(ConnectorFilter connectorFilter, int page, int size, String accountIdentifier);

  ConnectorDTO create(ConnectorRequestDTO connector, String accountIdentifier);

  ConnectorDTO update(ConnectorRequestDTO connectorRequestDTO, String accountIdentifier);

  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);

  ConnectorValidationResult validate(ConnectorRequestDTO connector, String accountIdentifier);

  boolean validateTheIdentifierIsUnique(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);

  ConnectorValidationResult testConnection(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);
}
