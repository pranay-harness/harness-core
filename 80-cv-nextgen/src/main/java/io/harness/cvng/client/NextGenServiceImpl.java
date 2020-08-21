package io.harness.cvng.client;

import com.google.inject.Inject;

import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestDTO;

import java.util.Optional;

public class NextGenServiceImpl implements NextGenService {
  @Inject NextGenClient nextGenClient;
  @Inject RequestExecutor requestExecutor;

  @Override
  public ConnectorDTO create(ConnectorRequestDTO connectorRequestDTO, String accountIdentifier) {
    return requestExecutor.execute(nextGenClient.create(connectorRequestDTO, accountIdentifier)).getData();
  }

  @Override
  public Optional<ConnectorDTO> get(
      String accountIdentifier, String connectorIdentifier, String orgIdentifier, String projectIdentifier) {
    return requestExecutor
        .execute(nextGenClient.get(accountIdentifier, connectorIdentifier, orgIdentifier, projectIdentifier))
        .getData();
  }
}
