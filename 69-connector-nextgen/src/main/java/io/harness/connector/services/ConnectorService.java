package io.harness.connector.services;

import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.connector.apis.dto.ConnectorSummaryDTO;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.ng.core.NGAccess;
import io.harness.security.encryption.EncryptedDataDetail;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;

public interface ConnectorService {
  Optional<ConnectorDTO> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);

  Page<ConnectorSummaryDTO> list(int page, int size, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, String type);

  ConnectorDTO create(ConnectorRequestDTO connector, String accountIdentifier);

  ConnectorDTO update(ConnectorRequestDTO connectorRequestDTO, String accountIdentifier);

  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);

  ConnectorValidationResult validate(ConnectorRequestDTO connector, String accountIdentifier);

  boolean validateTheIdentifierIsUnique(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);

  ConnectorValidationResult testConnection(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);

  List<EncryptedDataDetail> getEncryptionDataDetails(@Nonnull ConnectorDTO connectorDTO, @Nonnull NGAccess ngAccess);
}
