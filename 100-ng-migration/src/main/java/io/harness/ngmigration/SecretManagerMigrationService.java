package io.harness.ngmigration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretManagerConfig;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.ngmigration.connector.SecretFactory;
import io.harness.serializer.JsonUtils;

import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;
import software.wings.ngmigration.DiscoveryNode;
import software.wings.ngmigration.NGMigrationEntity;
import software.wings.ngmigration.NGMigrationEntityType;
import software.wings.ngmigration.NGMigrationStatus;
import software.wings.ngmigration.NGYamlFile;
import software.wings.ngmigration.NgMigration;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
public class SecretManagerMigrationService implements NgMigration {
  @Inject private SecretManager secretManager;

  @Override
  public DiscoveryNode discover(NGMigrationEntity entity) {
    SecretManagerConfig managerConfig = (SecretManagerConfig) entity;
    String entityId = managerConfig.getUuid();
    CgEntityId managerEntityId = CgEntityId.builder().type(NGMigrationEntityType.SECRET_MANAGER).id(entityId).build();
    CgEntityNode secretManagerNode = CgEntityNode.builder()
                                         .id(entityId)
                                         .type(NGMigrationEntityType.SECRET_MANAGER)
                                         .entityId(managerEntityId)
                                         .entity(managerConfig)
                                         .build();
    Set<CgEntityId> children = new HashSet<>();
    return DiscoveryNode.builder().children(children).entityNode(secretManagerNode).build();
  }

  @Override
  public DiscoveryNode discover(String accountId, String appId, String entityId) {
    return discover(secretManager.getSecretManager(accountId, entityId));
  }

  @Override
  public NGMigrationStatus canMigrate(
      Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId) {
    return null;
  }

  @Override
  public void migrate(
      Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId) {}

  @Override
  public List<NGYamlFile> getYamls(
      Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId) {
    SecretManagerConfig secretManagerConfig = (SecretManagerConfig) entities.get(entityId).getEntity();
    List<NGYamlFile> files = new ArrayList<>();
    files.add(NGYamlFile.builder()
                  .filename("connector/" + secretManagerConfig.getName() + ".yaml")
                  .yaml(JsonUtils.asTree(
                      ConnectorDTO.builder()
                          .connectorInfo(ConnectorInfoDTO.builder()
                                             .name(secretManagerConfig.getName())
                                             .identifier(secretManagerConfig.getName())
                                             .description(null)
                                             .tags(null)
                                             .orgIdentifier("__ORG_INPUT_REQUIRED__")
                                             .projectIdentifier("__PROJECT_INPUT_REQUIRED__")
                                             .connectorType(SecretFactory.getConnectorType(secretManagerConfig))
                                             .connectorConfig(SecretFactory.getConfigDTO(secretManagerConfig))
                                             .build())
                          .build()))
                  .build());
    return files;
  }
}
