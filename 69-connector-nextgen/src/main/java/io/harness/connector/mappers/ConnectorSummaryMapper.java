package io.harness.connector.mappers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.connector.apis.dto.ConnectorConfigSummaryDTO;
import io.harness.connector.apis.dto.ConnectorSummaryDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.gitconnector.GitConfig;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.mappers.kubernetesMapper.KubernetesConfigSummaryMapper;
import io.harness.exception.UnsupportedOperationException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class ConnectorSummaryMapper {
  private KubernetesConfigSummaryMapper kubernetesConfigSummaryMapper;
  private GitConfigSummaryMapper gitConfigSummaryMapper;
  public ConnectorSummaryDTO writeConnectorSummaryDTO(Connector connector) {
    return ConnectorSummaryDTO.builder()
        .name(connector.getName())
        .description(connector.getDescription())
        .identifier(connector.getIdentifier())
        .categories(connector.getCategories())
        .type(connector.getType())
        .connectorDetials(createConnectorDetailsDTO(connector))
        .tags(connector.getTags())
        .createdAt(connector.getCreatedAt())
        .lastModifiedAt(connector.getLastModifiedAt())
        .version(connector.getVersion())
        .tags(connector.getTags())
        .build();
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
