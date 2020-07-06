package io.harness.connector.mappers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.connector.apis.dto.ConnectorConfigSummaryDTO;
import io.harness.connector.apis.dto.ConnectorSummaryDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.mappers.kubernetesMapper.KubernetesConfigSummaryMapper;
import io.harness.exception.UnsupportedOperationException;

@Singleton
public class ConnectorSummaryMapper {
  @Inject private KubernetesConfigSummaryMapper kubernetesConfigSummaryMapper;
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
    switch (connector.getType()) {
      case KUBERNETES_CLUSTER:
        return kubernetesConfigSummaryMapper.createKubernetesConfigSummaryDTO((KubernetesClusterConfig) connector);
      default:
        throw new UnsupportedOperationException(
            String.format("The connector type [%s] is invalid", connector.getType()));
    }
  }
}
