/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.SecretManagerConfig;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretRequestWrapper;
import io.harness.ng.core.dto.secrets.SecretTextSpecDTO;
import io.harness.ngmigration.beans.MigrationInputDTO;
import io.harness.ngmigration.beans.NgEntityDetail;
import io.harness.secretmanagerclient.ValueType;
import io.harness.secrets.SecretService;

import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;
import software.wings.ngmigration.DiscoveryNode;
import software.wings.ngmigration.NGMigrationEntity;
import software.wings.ngmigration.NGMigrationEntityType;
import software.wings.ngmigration.NGMigrationStatus;
import software.wings.ngmigration.NGYamlFile;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
public class SecretMigrationService implements NgMigration {
  @Inject private SecretService secretService;

  @Override
  public DiscoveryNode discover(NGMigrationEntity entity) {
    EncryptedData encryptedData = (EncryptedData) entity;
    String entityId = encryptedData.getUuid();
    CgEntityId connectorEntityId = CgEntityId.builder().type(NGMigrationEntityType.SECRET).id(entityId).build();
    CgEntityNode connectorNode = CgEntityNode.builder()
                                     .id(entityId)
                                     .type(NGMigrationEntityType.SECRET)
                                     .entityId(connectorEntityId)
                                     .entity(encryptedData)
                                     .build();
    Set<CgEntityId> children = new HashSet<>();
    children.add(CgEntityId.builder().type(NGMigrationEntityType.SECRET_MANAGER).id(encryptedData.getKmsId()).build());
    return DiscoveryNode.builder().children(children).entityNode(connectorNode).build();
  }

  @Override
  public DiscoveryNode discover(String accountId, String appId, String entityId) {
    return discover(secretService.getSecretById(accountId, entityId).orElse(null));
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
    EncryptedData encryptedData = (EncryptedData) entities.get(entityId).getEntity();
    SecretManagerConfig secretManagerConfig =
        (SecretManagerConfig) entities
            .get(CgEntityId.builder().type(NGMigrationEntityType.SECRET_MANAGER).id(encryptedData.getKmsId()).build())
            .getEntity();
    List<NGYamlFile> files = new ArrayList<>();
    files.add(NGYamlFile.builder()
                  .filename("secret/" + encryptedData.getName() + ".yaml")
                  .yaml(SecretRequestWrapper.builder()
                            .secret(SecretDTOV2.builder()
                                        .name(encryptedData.getName())
                                        .identifier(encryptedData.getName())
                                        .description(null)
                                        .orgIdentifier(inputDTO.getOrgIdentifier())
                                        .projectIdentifier(inputDTO.getProjectIdentifier())
                                        .spec(SecretTextSpecDTO.builder()
                                                  .valueType(ValueType.Inline)
                                                  .value("__ACTUAL_SECRET__")
                                                  .secretManagerIdentifier(secretManagerConfig.getName())
                                                  .build())
                                        .build())
                            .build())
                  .build());
    return files;
  }
}
