/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service;

import static io.harness.beans.EnvironmentType.PROD;
import static io.harness.ng.core.environment.beans.EnvironmentType.PreProduction;
import static io.harness.ng.core.environment.beans.EnvironmentType.Production;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ngmigration.beans.MigrationInputDTO;
import io.harness.ngmigration.beans.NgEntityDetail;

import software.wings.beans.Environment;
import software.wings.infra.InfrastructureDefinition;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;
import software.wings.ngmigration.DiscoveryNode;
import software.wings.ngmigration.NGMigrationEntity;
import software.wings.ngmigration.NGMigrationEntityType;
import software.wings.ngmigration.NGMigrationStatus;
import software.wings.ngmigration.NGYamlFile;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureDefinitionService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.CDC)
public class EnvironmentMigrationService implements NgMigration {
  @Inject private EnvironmentService environmentService;
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;

  @Override
  public DiscoveryNode discover(NGMigrationEntity entity) {
    Environment environment = (Environment) entity;
    String entityId = environment.getUuid();
    CgEntityId environmentEntityId = CgEntityId.builder().type(NGMigrationEntityType.ENVIRONMENT).id(entityId).build();
    CgEntityNode environmentNode = CgEntityNode.builder()
                                       .id(entityId)
                                       .type(NGMigrationEntityType.ENVIRONMENT)
                                       .entityId(environmentEntityId)
                                       .entity(environment)
                                       .build();

    Set<CgEntityId> children = new HashSet<>();
    List<InfrastructureDefinition> infraDefs = infrastructureDefinitionService.getNameAndIdForEnvironments(
        environment.getAppId(), Collections.singletonList(entityId));
    if (EmptyPredicate.isNotEmpty(infraDefs)) {
      children.addAll(
          infraDefs.stream()
              .map(infra -> CgEntityId.builder().id(infra.getUuid()).type(NGMigrationEntityType.INFRA).build())
              .collect(Collectors.toSet()));
    }
    return DiscoveryNode.builder().children(children).entityNode(environmentNode).build();
  }

  @Override
  public DiscoveryNode discover(String accountId, String appId, String entityId) {
    return discover(environmentService.get(appId, entityId));
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

  public EnvironmentYaml getEnvironmentYaml(MigrationInputDTO inputDTO, Map<CgEntityId, CgEntityNode> entities,
      Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId) {
    Environment environment = (Environment) entities.get(entityId).getEntity();
    EnvironmentType environmentType = (environment.getEnvironmentType() == PROD) ? Production : PreProduction;
    return EnvironmentYaml.builder()
        .name(environment.getName())
        .identifier(environment.getName())
        .type(environmentType)
        .tags(null)
        .build();
  }
}
