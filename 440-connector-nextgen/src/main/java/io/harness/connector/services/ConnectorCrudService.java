package io.harness.connector.services;

import io.harness.connector.ConnectorCategory;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.apis.dto.ConnectorCatalogueResponseDTO;
import io.harness.connector.apis.dto.ConnectorFilterPropertiesDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationResult;

import java.util.Optional;
import org.springframework.data.domain.Page;

public interface ConnectorCrudService {
  Page<ConnectorResponseDTO> list(int page, int size, String accountIdentifier,
      ConnectorFilterPropertiesDTO filterProperties, String orgIdentifier, String projectIdentifier,
      String filterIdentifier, String searchTerm, Boolean includeAllConnectorsAccessibleAtScope);

  Page<ConnectorResponseDTO> list(int page, int size, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, ConnectorType type, ConnectorCategory category);

  Optional<ConnectorResponseDTO> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);

  ConnectorResponseDTO create(ConnectorDTO connector, String accountIdentifier);

  ConnectorResponseDTO update(ConnectorDTO connectorRequestDTO, String accountIdentifier);

  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);

  ConnectorCatalogueResponseDTO getConnectorCatalogue();

  void updateConnectorEntityWithPerpetualtaskId(String accountIdentifier, ConnectorInfoDTO connector, String id);

  void updateConnectivityDetailOfTheConnector(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, ConnectorValidationResult connectorValidationResult);
}
