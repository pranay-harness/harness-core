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
import io.harness.connector.entities.embedded.gitconnector.GitConfig;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.mappers.kubernetesMapper.KubernetesConfigSummaryMapper;
import io.harness.exception.UnsupportedOperationException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Map;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class ConnectorSummaryMapper {
  private KubernetesConfigSummaryMapper kubernetesConfigSummaryMapper;
  private GitConfigSummaryMapper gitConfigSummaryMapper;
  private static final String EMPTY_STRING = "";

  public ConnectorSummaryDTO writeConnectorSummaryDTO(Connector connector, String accountName,
      Map<String, String> orgIdentifierOrgNameMap, Map<String, String> projectIdentifierProjectNameMap) {
    ConnectorSummaryDTOBuilder connectorSummaryBuilder = ConnectorSummaryDTO.builder()
                                                             .name(connector.getName())
                                                             .description(connector.getDescription())
                                                             .identifier(connector.getIdentifier())
                                                             .accountName(accountName)
                                                             .categories(connector.getCategories())
                                                             .type(connector.getType())
                                                             .connectorDetials(createConnectorDetailsDTO(connector))
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
    // todo @deepak: Change this design to something so that switch case is not required
    switch (connector.getType()) {
      case KUBERNETES_CLUSTER:
        return kubernetesConfigSummaryMapper.createKubernetesConfigSummaryDTO((KubernetesClusterConfig) connector);
      case GIT:
        return gitConfigSummaryMapper.createGitConfigSummaryDTO((GitConfig) connector);
      default:
        throw new UnsupportedOperationException(
            String.format("The connector type [%s] is invalid", connector.getType()));
    }
  }
}
