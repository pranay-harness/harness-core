package io.harness.ngmigration;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.Service;
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
import software.wings.service.intfc.ServiceResourceService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.CDC)
public class ServiceMigrationService implements NgMigration {
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ArtifactStreamService artifactStreamService;

  @Override
  public DiscoveryNode discover(NGMigrationEntity entity) {
    Service service = (Service) entity;
    String serviceId = service.getUuid();
    CgEntityId serviceEntityId = CgEntityId.builder().type(NGMigrationEntityType.SERVICE).id(serviceId).build();
    CgEntityNode serviceEntityNode = CgEntityNode.builder()
                                         .id(serviceId)
                                         .type(NGMigrationEntityType.SERVICE)
                                         .entityId(serviceEntityId)
                                         .entity(service)
                                         .build();
    List<ArtifactStream> artifactStreams =
        artifactStreamService.getArtifactStreamsForService(service.getAppId(), serviceId);
    Set<CgEntityId> children = new HashSet<>();
    if (isNotEmpty(artifactStreams)) {
      children.addAll(artifactStreams.stream()
                          .map(artifactStream
                              -> CgEntityId.builder()
                                     .id(artifactStream.getUuid())
                                     .type(NGMigrationEntityType.ARTIFACT_STREAM)
                                     .build())
                          .collect(Collectors.toSet()));
    }
    return DiscoveryNode.builder().entityNode(serviceEntityNode).children(children).build();
  }

  @Override
  public DiscoveryNode discover(String accountId, String appId, String entityId) {
    return discover(serviceResourceService.get(entityId));
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
    return new ArrayList<>();
  }
}
