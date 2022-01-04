package io.harness.ccm.service.intf;

import io.harness.ccm.commons.entities.CCMConnectorDetails;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorResponseDTO;

import java.util.List;

public interface CCMConnectorDetailsService {
  List<ConnectorResponseDTO> listNgConnectors(String accountId, ConnectivityStatus status);
  CCMConnectorDetails getFirstConnectorDetails(String accountId);
}
