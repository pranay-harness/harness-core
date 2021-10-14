package io.harness.ngmigration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.SecretManagerConfig;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.ng.core.dto.secrets.SecretTextSpecDTO;
import io.harness.secretmanagerclient.ValueType;
import io.harness.secrets.SecretService;
import io.harness.serializer.JsonUtils;

import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;
import software.wings.ngmigration.DiscoveryNode;
import software.wings.ngmigration.NGMigrationEntity;
import software.wings.ngmigration.NGMigrationEntityType;
import software.wings.ngmigration.NGMigrationStatus;
import software.wings.ngmigration.NGYamlFile;
import software.wings.ngmigration.NgMigration;

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
  public List<NGYamlFile> getYamls(
      Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId) {
    EncryptedData encryptedData = (EncryptedData) entities.get(entityId).getEntity();
    SecretManagerConfig secretManagerConfig =
        (SecretManagerConfig) entities
            .get(CgEntityId.builder().type(NGMigrationEntityType.SECRET_MANAGER).id(encryptedData.getKmsId()).build())
            .getEntity();
    List<NGYamlFile> files = new ArrayList<>();
    files.add(
        NGYamlFile.builder()
            .filename("secret/" + encryptedData.getName() + ".yaml")
            .yaml(JsonUtils.asTree(SecretResponseWrapper.builder()
                                       .secret(SecretDTOV2.builder()
                                                   .name(encryptedData.getName())
                                                   .identifier(encryptedData.getName())
                                                   .description(null)
                                                   .orgIdentifier("__ORG_INPUT_REQUIRED__")
                                                   .projectIdentifier("__PROJECT_INPUT_REQUIRED__")
                                                   .spec(SecretTextSpecDTO.builder()
                                                             .valueType(ValueType.Inline)
                                                             .value("__ACTUAL_SECRET__")
                                                             .secretManagerIdentifier(secretManagerConfig.getName())
                                                             .build())
                                                   .build())
                                       .build()))
            .build());
    return files;
  }
}
