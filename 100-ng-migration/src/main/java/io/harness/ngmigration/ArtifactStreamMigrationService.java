package io.harness.ngmigration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;
import software.wings.ngmigration.DiscoveryNode;
import software.wings.ngmigration.NGMigrationEntity;
import software.wings.ngmigration.NGMigrationEntityType;
import software.wings.ngmigration.NGMigrationStatus;
import software.wings.ngmigration.NGYamlFile;
import software.wings.ngmigration.NgMigration;
import software.wings.service.intfc.ArtifactStreamService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
public class ArtifactStreamMigrationService implements NgMigration {
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private NgMigrationFactory ngMigrationFactory;

  @Override
  public DiscoveryNode discover(NGMigrationEntity entity) {
    ArtifactStream artifactStream = (ArtifactStream) entity;
    String entityId = artifactStream.getUuid();
    CgEntityId artifactStreamEntityId =
        CgEntityId.builder().type(NGMigrationEntityType.ARTIFACT_STREAM).id(entityId).build();
    CgEntityNode artifactStreamNode = CgEntityNode.builder()
                                          .id(entityId)
                                          .type(NGMigrationEntityType.ARTIFACT_STREAM)
                                          .entityId(artifactStreamEntityId)
                                          .entity(artifactStream)
                                          .build();
    Set<CgEntityId> children = Collections.singleton(
        CgEntityId.builder().type(NGMigrationEntityType.CONNECTOR).id(artifactStream.getSettingId()).build());
    return DiscoveryNode.builder().children(children).entityNode(artifactStreamNode).build();
  }

  @Override
  public DiscoveryNode discover(String accountId, String appId, String entityId) {
    return discover(artifactStreamService.get(entityId));
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
    List<NGYamlFile> files = new ArrayList<>();
    if (EmptyPredicate.isNotEmpty(graph.get(entityId))) {
      graph.get(entityId).forEach(entityId1 -> {
        files.addAll(ngMigrationFactory.getMethod(entityId1.getType()).getYamls(entities, graph, entityId1));
      });
    }
    return files;
  }
}
