package io.harness.connector.gitsync;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.common.EntityReference;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.entities.Connector;
import io.harness.connector.mappers.ConnectorMapper;
import io.harness.gitsync.entityInfo.EntityGitPersistenceHelperService;
import io.harness.ng.core.EntityDetail;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.DX)
public class ConnectorGitSyncHelper implements EntityGitPersistenceHelperService<Connector, ConnectorDTO> {
  private final ConnectorMapper connectorMapper;

  @Override
  public Supplier<ConnectorDTO> getYamlFromEntity(Connector entity) {
    return () -> ConnectorDTO.builder().connectorInfo(connectorMapper.getConnectorInfoDTO(entity)).build();
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.CONNECTORS;
  }

  @Override
  public Supplier<Connector> getEntityFromYaml(ConnectorDTO yaml) {
    return () -> connectorMapper.toConnector(yaml, "accountIdentifier");
  }

  @Override
  public EntityDetail getEntityDetail(Connector entity) {
    return EntityDetail.builder()
        .name(entity.getName())
        .type(EntityType.CONNECTORS)
        .entityRef(IdentifierRef.builder()
                       .accountIdentifier(entity.getAccountIdentifier())
                       .orgIdentifier(entity.getOrgIdentifier())
                       .projectIdentifier(entity.getProjectIdentifier())
                       .build())
        .build();
  }

  @Override
  public ConnectorDTO save(ConnectorDTO yaml, String accountIdentifier) {
    return null;
  }

  @Override
  public ConnectorDTO update(ConnectorDTO yaml, String accountIdentifier) {
    return null;
  }

  @Override
  public boolean delete(EntityReference entityReference) {
    return false;
  }
}
