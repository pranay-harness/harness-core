/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.ngmigration.NGMigrationEntityType.ARTIFACT_STREAM;
import static software.wings.ngmigration.NGMigrationEntityType.CONNECTOR;
import static software.wings.ngmigration.NGMigrationEntityType.SERVICE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.PrimaryArtifact;
import io.harness.cdng.service.beans.KubernetesServiceSpec;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.cdng.service.beans.ServiceYaml;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.ngmigration.beans.MigrationInputDTO;
import io.harness.ngmigration.beans.NgEntityDetail;
import io.harness.pms.yaml.ParameterField;

import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;
import software.wings.ngmigration.DiscoveryNode;
import software.wings.ngmigration.NGMigrationEntity;
import software.wings.ngmigration.NGMigrationStatus;
import software.wings.ngmigration.NGYamlFile;
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
    CgEntityId serviceEntityId = CgEntityId.builder().type(SERVICE).id(serviceId).build();
    CgEntityNode serviceEntityNode =
        CgEntityNode.builder().id(serviceId).type(SERVICE).entityId(serviceEntityId).entity(service).build();
    List<ArtifactStream> artifactStreams =
        artifactStreamService.getArtifactStreamsForService(service.getAppId(), serviceId);
    Set<CgEntityId> children = new HashSet<>();
    if (isNotEmpty(artifactStreams)) {
      children.addAll(
          artifactStreams.stream()
              .map(artifactStream -> CgEntityId.builder().id(artifactStream.getUuid()).type(ARTIFACT_STREAM).build())
              .collect(Collectors.toSet()));
    }
    return DiscoveryNode.builder().entityNode(serviceEntityNode).children(children).build();
  }

  @Override
  public DiscoveryNode discover(String accountId, String appId, String entityId) {
    return discover(serviceResourceService.get(entityId));
  }

  private PrimaryArtifact getPrimaryArtifact(
      ArtifactStream artifactStream, Map<CgEntityId, NgEntityDetail> migratedEntities) {
    if (artifactStream instanceof DockerArtifactStream) {
      DockerArtifactStream dockerArtifactStream = (DockerArtifactStream) artifactStream;
      NgEntityDetail connector =
          migratedEntities.get(CgEntityId.builder().type(CONNECTOR).id(dockerArtifactStream.getSettingId()).build());
      return PrimaryArtifact.builder()
          .sourceType(ArtifactSourceType.DOCKER_REGISTRY)
          .spec(DockerHubArtifactConfig.builder()
                    .connectorRef(ParameterField.createValueField(connector.getIdentifier()))
                    .imagePath(ParameterField.createValueField(dockerArtifactStream.getImageName()))
                    .tag(ParameterField.createValueField("<+input>"))
                    .build())
          .build();
    }
    throw new UnsupportedOperationException("Only Docker Artifact Streams are supported");
  }

  public ServiceConfig getServiceConfig(MigrationInputDTO inputDTO, Map<CgEntityId, CgEntityNode> entities,
      Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId, Map<CgEntityId, NgEntityDetail> migratedEntities) {
    Service service = (Service) entities.get(entityId).getEntity();
    PrimaryArtifact primaryArtifact = null;
    if (isNotEmpty(graph.get(entityId)) && graph.get(entityId).stream().anyMatch(e -> e.getType() == ARTIFACT_STREAM)) {
      CgEntityId artifactStreamId =
          graph.get(entityId)
              .stream()
              .filter(e -> e.getType() == ARTIFACT_STREAM)
              .findFirst()
              .orElseThrow(() -> new UnsupportedOperationException("This should not be thrown"));
      ArtifactStream artifactStream = (ArtifactStream) entities.get(artifactStreamId).getEntity();
      primaryArtifact = getPrimaryArtifact(artifactStream, migratedEntities);
    }
    ServiceDefinition serviceDefinition =
        ServiceDefinition.builder()
            .type(ServiceDefinitionType.KUBERNETES)
            .serviceSpec(KubernetesServiceSpec.builder()
                             .variables(new ArrayList<>())
                             .artifacts(ArtifactListConfig.builder().primary(primaryArtifact).build())
                             .build())
            .build();

    ServiceYaml serviceYaml = ServiceYaml.builder()
                                  .name(MigratorUtility.generateIdentifier(service.getName()))
                                  .identifier(service.getName())
                                  .description(ParameterField.createValueField(service.getDescription()))
                                  .tags(null)
                                  .build();
    return ServiceConfig.builder().service(serviceYaml).serviceDefinition(serviceDefinition).build();
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
    return new ArrayList<>();
  }
}
