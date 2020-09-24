package io.harness.connector.mappers.gcpmappers;

import com.google.inject.Singleton;

import io.harness.connector.apis.dto.gcpconnector.GcpConnectorSummaryDTO;
import io.harness.connector.entities.embedded.gcpconnector.GcpConfig;
import io.harness.connector.mappers.ConnectorConfigSummaryDTOMapper;

@Singleton
public class GcpConnectorSummaryMapper implements ConnectorConfigSummaryDTOMapper<GcpConfig> {
  public GcpConnectorSummaryDTO toConnectorConfigSummaryDTO(GcpConfig connector) {
    return GcpConnectorSummaryDTO.builder().build();
  }
}