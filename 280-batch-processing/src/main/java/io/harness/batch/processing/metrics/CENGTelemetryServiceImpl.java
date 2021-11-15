package io.harness.batch.processing.metrics;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.utils.RestCallToNGManagerClientUtils.execute;

import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.filter.FilterType;
import io.harness.ng.beans.PageResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class CENGTelemetryServiceImpl implements CENGTelemetryService {
  @Autowired private ConnectorResourceClient connectorResourceClient;

  public List<CEConnectorsTelemetry> getNextGenConnectorsCountByType(String accountId) {
    List<ConnectorResponseDTO> nextGenConnectors = new ArrayList<>();
    PageResponse<ConnectorResponseDTO> response = null;
    ConnectorFilterPropertiesDTO connectorFilterPropertiesDTO =
        ConnectorFilterPropertiesDTO.builder()
            .types(Arrays.asList(ConnectorType.CE_KUBERNETES_CLUSTER, ConnectorType.CE_AWS, ConnectorType.CE_AZURE,
                ConnectorType.GCP_CLOUD_COST, ConnectorType.KUBERNETES_CLUSTER))
            .build();
    connectorFilterPropertiesDTO.setFilterType(FilterType.CONNECTOR);
    int page = 0;
    int size = 100;
    do {
      response = getConnectors(accountId, page, size, connectorFilterPropertiesDTO);
      if (response != null && isNotEmpty(response.getContent())) {
        nextGenConnectors.addAll(response.getContent());
      }
      page++;
    } while (response != null && isNotEmpty(response.getContent()));

    return nextGenConnectors.stream()
            .map(connector -> new CEConnectorsTelemetry(connector.getConnector().getConnectorType(), connector.getStatus().getStatus()))
            .collect(Collectors.toList());
  }

  private PageResponse getConnectors(
      String accountId, int page, int size, ConnectorFilterPropertiesDTO connectorFilterPropertiesDTO) {
    return execute(
        connectorResourceClient.listConnectors(accountId, null, null, page, size, connectorFilterPropertiesDTO, false));
  }
}
