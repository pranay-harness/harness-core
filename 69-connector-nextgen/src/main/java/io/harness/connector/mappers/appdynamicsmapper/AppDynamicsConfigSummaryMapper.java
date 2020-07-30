package io.harness.connector.mappers.appdynamicsmapper;

import com.google.inject.Singleton;

import io.harness.connector.apis.dto.appdynamicsconnector.AppDynamicsConfigSummaryDTO;
import io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConfig;
import io.harness.connector.mappers.ConnectorConfigSummaryDTOMapper;

@Singleton
public class AppDynamicsConfigSummaryMapper implements ConnectorConfigSummaryDTOMapper<AppDynamicsConfig> {
  public AppDynamicsConfigSummaryDTO toConnectorConfigSummaryDTO(AppDynamicsConfig connector) {
    return AppDynamicsConfigSummaryDTO.builder()
        .username(connector.getUsername())
        .accountname(connector.getAccountname())
        .controllerUrl(connector.getControllerUrl())
        .build();
  }
}
