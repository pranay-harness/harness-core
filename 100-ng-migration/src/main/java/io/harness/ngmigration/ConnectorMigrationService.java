package io.harness.ngmigration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.ngmigration.connector.ConnectorFactory;
import io.harness.serializer.JsonUtils;

import software.wings.beans.SettingAttribute;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;
import software.wings.ngmigration.DiscoveryNode;
import software.wings.ngmigration.NGMigrationEntity;
import software.wings.ngmigration.NGMigrationEntityType;
import software.wings.ngmigration.NGMigrationStatus;
import software.wings.ngmigration.NGYamlFile;
import software.wings.ngmigration.NgMigration;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
public class ConnectorMigrationService implements NgMigration {
  @Inject private SettingsService settingsService;

  @Override
  public DiscoveryNode discover(NGMigrationEntity entity) {
    SettingAttribute settingAttribute = (SettingAttribute) entity;
    String entityId = settingAttribute.getUuid();
    CgEntityId connectorEntityId = CgEntityId.builder().type(NGMigrationEntityType.CONNECTOR).id(entityId).build();
    CgEntityNode connectorNode = CgEntityNode.builder()
                                     .id(entityId)
                                     .type(NGMigrationEntityType.CONNECTOR)
                                     .entityId(connectorEntityId)
                                     .entity(settingAttribute)
                                     .build();
    Set<CgEntityId> children = new HashSet<>();
    return DiscoveryNode.builder().children(children).entityNode(connectorNode).build();
  }

  @Override
  public DiscoveryNode discover(String accountId, String appId, String entityId) {
    return discover(settingsService.getByAccountAndId(accountId, entityId));
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
    SettingAttribute settingAttribute = (SettingAttribute) entities.get(entityId).getEntity();
    List<NGYamlFile> files = new ArrayList<>();
    files.add(NGYamlFile.builder()
                  .filename("connector/" + settingAttribute.getName() + ".yaml")
                  .yaml(JsonUtils.asTree(
                      ConnectorDTO.builder()
                          .connectorInfo(ConnectorInfoDTO.builder()
                                             .name(settingAttribute.getName())
                                             .identifier(settingAttribute.getName())
                                             .description(null)
                                             .tags(null)
                                             .orgIdentifier("__ORG_INPUT_REQUIRED__")
                                             .projectIdentifier("__PROJECT_INPUT_REQUIRED__")
                                             .connectorType(ConnectorFactory.getConnectorType(settingAttribute))
                                             .connectorConfig(ConnectorFactory.getConfigDTO(settingAttribute))
                                             .build())
                          .build()))
                  .build());
    return files;
  }
}
