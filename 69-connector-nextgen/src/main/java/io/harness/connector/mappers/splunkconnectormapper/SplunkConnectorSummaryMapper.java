package io.harness.connector.mappers.splunkconnectormapper;

import com.google.inject.Singleton;

import io.harness.connector.apis.dto.splunkconnector.SplunkConnectorSummaryDTO;
import io.harness.connector.entities.embedded.splunkconnector.SplunkConnector;
import io.harness.connector.mappers.ConnectorConfigSummaryDTOMapper;

@Singleton
public class SplunkConnectorSummaryMapper implements ConnectorConfigSummaryDTOMapper<SplunkConnector> {
  public SplunkConnectorSummaryDTO toConnectorConfigSummaryDTO(SplunkConnector connector) {
    return SplunkConnectorSummaryDTO.builder()
        .username(connector.getUsername())
        .splunkUrl(connector.getSplunkUrl())
        .build();
  }
}
