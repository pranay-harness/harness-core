package io.harness.connector.mappers.docker;

import com.google.inject.Singleton;

import io.harness.connector.apis.dto.docker.DockerConnectorSummaryDTO;
import io.harness.connector.entities.embedded.docker.DockerConnector;
import io.harness.connector.mappers.ConnectorConfigSummaryDTOMapper;

@Singleton
public class DockerConnectorSummaryMapper implements ConnectorConfigSummaryDTOMapper<DockerConnector> {
  @Override
  public DockerConnectorSummaryDTO toConnectorConfigSummaryDTO(DockerConnector connector) {
    return DockerConnectorSummaryDTO.builder().dockerRegistryUrl(connector.getUrl()).build();
  }
}
