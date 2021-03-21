package io.harness.connector.mappers.cek8s;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.cek8s.CEK8sDetails;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.cek8s.CEKubernetesClusterConfigDTO;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(DX)
public class CEKubernetesEntityToDTO implements ConnectorEntityToDTOMapper<CEKubernetesClusterConfigDTO, CEK8sDetails> {
  @Override
  public CEKubernetesClusterConfigDTO createConnectorDTO(CEK8sDetails connector) {
    return CEKubernetesClusterConfigDTO.builder().connectorRef(connector.getConnectorRef()).build();
  }
}
