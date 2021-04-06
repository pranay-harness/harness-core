package io.harness.gitsync.common.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.exception.UnexpectedException;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Singleton
@OwnedBy(DX)
@Slf4j
public class GitSyncConnectorHelper {
  ConnectorService connectorService;

  @Inject
  public GitSyncConnectorHelper(@Named("connectorDecoratorService") ConnectorService connectorService) {
    this.connectorService = connectorService;
  }

  public ScmConnector getConnectorAssociatedWithGitSyncConfig(YamlGitConfigDTO gitSyncConfigDTO, String accountId) {
    final String connectorRef = gitSyncConfigDTO.getGitConnectorRef();
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
        connectorRef, accountId, gitSyncConfigDTO.getOrganizationIdentifier(), gitSyncConfigDTO.getProjectIdentifier());
    Optional<ConnectorResponseDTO> connectorDTO = connectorService.get(accountId, identifierRef.getOrgIdentifier(),
        identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
    if (connectorDTO.isPresent()) {
      ConnectorInfoDTO connector = connectorDTO.get().getConnector();
      ConnectorConfigDTO connectorConfig = connector.getConnectorConfig();
      if (connectorConfig instanceof ScmConnector) {
        return (ScmConnector) connector.getConnectorConfig();
      }
      throw new UnexpectedException(
          String.format("The connector with thhe  id %s, accountId %s, orgId %s, projectId %s is not a scm connector",
              gitSyncConfigDTO.getIdentifier(), accountId, gitSyncConfigDTO.getOrganizationIdentifier(),
              gitSyncConfigDTO.getProjectIdentifier()));
    } else {
      throw new UnexpectedException(String.format(
          "No connector found with the id %s, accountId %s, orgId %s, projectId %s", gitSyncConfigDTO.getIdentifier(),
          accountId, gitSyncConfigDTO.getOrganizationIdentifier(), gitSyncConfigDTO.getProjectIdentifier()));
    }
  }
}
