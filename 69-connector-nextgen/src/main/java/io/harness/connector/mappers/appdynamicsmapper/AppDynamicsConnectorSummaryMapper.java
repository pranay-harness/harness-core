package io.harness.connector.mappers.appdynamicsmapper;

import com.google.inject.Singleton;

import io.harness.connector.apis.dto.appdynamicsconnector.AppDynamicsConnectorSummaryDTO;
import io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConnector;
import io.harness.connector.mappers.ConnectorConfigSummaryDTOMapper;

@Singleton
public class AppDynamicsConnectorSummaryMapper implements ConnectorConfigSummaryDTOMapper<AppDynamicsConnector> {
  public AppDynamicsConnectorSummaryDTO toConnectorConfigSummaryDTO(AppDynamicsConnector connector) {
    return AppDynamicsConnectorSummaryDTO.builder()
        .username(connector.getUsername())
        .accountname(connector.getAccountname())
        .controllerUrl(connector.getControllerUrl())
        .build();
  }
}
