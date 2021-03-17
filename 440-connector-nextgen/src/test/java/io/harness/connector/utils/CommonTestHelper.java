package io.harness.connector.utils;

import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CommonTestHelper {
  public String connectorIdentifier = "identifier_ph";
  public String name = "name_ph";
  public String description = "description_ph";
  public String projectIdentifier = "projectIdentifier_ph";
  public String orgIdentifier = "orgIdentifier_ph";
  public Map<String, String> tags = ImmutableMap.of("company", "Harness", "env", "dev");

  public ConnectorDTO createConnectorDTO(ConnectorType connectorType, ConnectorConfigDTO connectorConfigDTO) {
    final ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder()
                                                  .connectorConfig(connectorConfigDTO)
                                                  .connectorType(connectorType)
                                                  .description(description)
                                                  .identifier(connectorIdentifier)
                                                  .name(name)
                                                  .orgIdentifier(orgIdentifier)
                                                  .projectIdentifier(projectIdentifier)
                                                  .tags(tags)
                                                  .build();
    return ConnectorDTO.builder().connectorInfo(connectorInfoDTO).build();
  }
}
