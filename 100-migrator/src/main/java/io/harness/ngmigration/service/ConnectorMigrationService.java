/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.ngmigration.beans.MigrationInputDTO;
import io.harness.ngmigration.beans.NgEntityDetail;
import io.harness.ngmigration.connector.ConnectorFactory;

import software.wings.beans.SettingAttribute;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;
import software.wings.ngmigration.DiscoveryNode;
import software.wings.ngmigration.NGMigrationEntity;
import software.wings.ngmigration.NGMigrationEntityType;
import software.wings.ngmigration.NGMigrationStatus;
import software.wings.ngmigration.NGYamlFile;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

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
    String secret = ConnectorFactory.getSecretId(settingAttribute);
    if (StringUtils.isNotBlank(secret)) {
      children.add(CgEntityId.builder().id(secret).type(NGMigrationEntityType.SECRET).build());
    }
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
  public List<NGYamlFile> getYamls(MigrationInputDTO inputDTO, Map<CgEntityId, CgEntityNode> entities,
      Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId, Map<CgEntityId, NgEntityDetail> migratedEntities) {
    SettingAttribute settingAttribute = (SettingAttribute) entities.get(entityId).getEntity();
    List<NGYamlFile> files = new ArrayList<>();
    String identifier = MigratorUtility.generateIdentifier(settingAttribute.getName());
    files.add(NGYamlFile.builder()
                  .filename("connector/" + settingAttribute.getName() + ".yaml")
                  .yaml(ConnectorDTO.builder()
                            .connectorInfo(ConnectorInfoDTO.builder()
                                               .name(settingAttribute.getName())
                                               .identifier(identifier)
                                               .description(null)
                                               .tags(null)
                                               .orgIdentifier(inputDTO.getOrgIdentifier())
                                               .projectIdentifier(inputDTO.getProjectIdentifier())
                                               .connectorType(ConnectorFactory.getConnectorType(settingAttribute))
                                               .connectorConfig(ConnectorFactory.getConfigDTO(settingAttribute))
                                               .build())
                            .build())
                  .build());
    migratedEntities.putIfAbsent(entityId,
        NgEntityDetail.builder()
            .identifier(identifier)
            .orgIdentifier(inputDTO.getOrgIdentifier())
            .projectIdentifier(inputDTO.getProjectIdentifier())
            .build());
    return files;
  }
}
