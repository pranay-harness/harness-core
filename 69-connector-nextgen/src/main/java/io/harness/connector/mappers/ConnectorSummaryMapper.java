package io.harness.connector.mappers;

import static io.harness.connector.entities.Connector.Scope.ORGANIZATION;
import static io.harness.connector.entities.Connector.Scope.PROJECT;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.connector.apis.dto.ConnectorConfigSummaryDTO;
import io.harness.connector.apis.dto.ConnectorSummaryDTO;
import io.harness.connector.apis.dto.ConnectorSummaryDTO.ConnectorSummaryDTOBuilder;
import io.harness.connector.entities.Connector;
import io.harness.connector.mappers.appdynamicsmapper.AppDynamicsConfigSummaryMapper;
import io.harness.connector.mappers.gitconnectormapper.GitConfigSummaryMapper;
import io.harness.connector.mappers.kubernetesMapper.KubernetesConfigSummaryMapper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Map;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class ConnectorSummaryMapper {
  private KubernetesConfigSummaryMapper kubernetesConfigSummaryMapper;
  private GitConfigSummaryMapper gitConfigSummaryMapper;
  private AppDynamicsConfigSummaryMapper appDynamicsConfigSummaryMapper;
  private static final String EMPTY_STRING = "";
  @Inject private Map<String, ConnectorConfigSummaryDTOMapper> connectorConfigSummaryDTOMapperMap;

  public ConnectorSummaryDTO writeConnectorSummaryDTO(Connector connector, String accountName,
      Map<String, String> orgIdentifierOrgNameMap, Map<String, String> projectIdentifierProjectNameMap) {
    ConnectorSummaryDTOBuilder connectorSummaryBuilder = ConnectorSummaryDTO.builder()
                                                             .name(connector.getName())
                                                             .description(connector.getDescription())
                                                             .identifier(connector.getIdentifier())
                                                             .accountName(accountName)
                                                             .categories(connector.getCategories())
                                                             .type(connector.getType())
                                                             .connectorDetails(createConnectorDetailsDTO(connector))
                                                             .tags(connector.getTags())
                                                             .createdAt(connector.getCreatedAt())
                                                             .lastModifiedAt(connector.getLastModifiedAt())
                                                             .version(connector.getVersion())
                                                             .tags(connector.getTags());
    if (connector.getScope() == ORGANIZATION) {
      connectorSummaryBuilder.orgName(getOrgNameFromMap(connector.getOrgIdentifier(), orgIdentifierOrgNameMap));
    } else if (connector.getScope() == PROJECT) {
      connectorSummaryBuilder.orgName(getOrgNameFromMap(connector.getOrgIdentifier(), orgIdentifierOrgNameMap));
      connectorSummaryBuilder.projectName(
          getProjectNameFromMap(connector.getProjectIdentifier(), projectIdentifierProjectNameMap));
    }
    return connectorSummaryBuilder.build();
  }

  private String getOrgNameFromMap(String orgIdentifier, Map<String, String> orgIdentifierOrgNameMap) {
    if (isEmpty(orgIdentifier)) {
      return EMPTY_STRING;
    }
    return orgIdentifierOrgNameMap.containsKey(orgIdentifier) ? orgIdentifierOrgNameMap.get(orgIdentifier)
                                                              : EMPTY_STRING;
  }

  private String getProjectNameFromMap(String projectIdentifier, Map<String, String> projectIdentifierProjectNameMap) {
    if (isEmpty(projectIdentifier)) {
      return EMPTY_STRING;
    }
    return projectIdentifierProjectNameMap.containsKey(projectIdentifier)
        ? projectIdentifierProjectNameMap.get(projectIdentifier)
        : EMPTY_STRING;
  }

  private ConnectorConfigSummaryDTO createConnectorDetailsDTO(Connector connector) {
    ConnectorConfigSummaryDTOMapper connectorDTOToEntityMapper =
        connectorConfigSummaryDTOMapperMap.get(connector.getType().toString());
    return connectorDTOToEntityMapper.toConnectorConfigSummaryDTO(connector);
  }
}
