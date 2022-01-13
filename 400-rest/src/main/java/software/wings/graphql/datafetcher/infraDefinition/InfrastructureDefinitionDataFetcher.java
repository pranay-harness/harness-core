/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.infraDefinition;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;

import software.wings.beans.Environment;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLInfrastructureDefinitionQueryParameters;
import software.wings.graphql.schema.type.QLInfrastructureDefinition;
import software.wings.infra.InfrastructureDefinition;
import software.wings.security.PermissionAttribute;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.InfrastructureDefinitionService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class InfrastructureDefinitionDataFetcher
    extends AbstractObjectDataFetcher<QLInfrastructureDefinition, QLInfrastructureDefinitionQueryParameters> {
  @Inject HPersistence persistence;
  @Inject InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject AuthService authService;
  private static final String INFRA_DOES_NOT_EXIST_MSG = "Infrastructure does not exist";
  private static final String INVALID_INPUT = "Input given is not valid";
  @Override
  @AuthRule(permissionType = LOGGED_IN)
  public QLInfrastructureDefinition fetch(QLInfrastructureDefinitionQueryParameters qlQuery, String accountId) {
    InfrastructureDefinition infrastructureDefinition = null;
    if (qlQuery.getInfrastructureId() != null) {
      infrastructureDefinition =
          infrastructureDefinitionService.getInfraDefById(accountId, qlQuery.getInfrastructureId());
    } else if (qlQuery.getInfrastructureName() != null) {
      infrastructureDefinition = infrastructureDefinitionService.getInfraByName(
          accountId, qlQuery.getInfrastructureName(), qlQuery.getEnvironmentId());
    } else {
      throw new InvalidRequestException(INVALID_INPUT, WingsException.USER);
    }
    if (infrastructureDefinition == null) {
      throw new InvalidRequestException(INFRA_DOES_NOT_EXIST_MSG, WingsException.USER);
    }
    final User user = UserThreadLocal.get();
    if (user != null) {
      authService.authorizeAppAccess(
          accountId, infrastructureDefinition.getAppId(), user, PermissionAttribute.Action.READ);
    }
    return createInfrastructureDefinition(infrastructureDefinition);
  }

  protected QLInfrastructureDefinition createInfrastructureDefinition(
      InfrastructureDefinition infrastructureDefinition) {
    return QLInfrastructureDefinition.builder()
        .id(infrastructureDefinition.getUuid())
        .name(infrastructureDefinition.getName())
        .deploymentType(infrastructureDefinition.getDeploymentType().getDisplayName())
        .scopedToServices(infrastructureDefinition.getScopedToServices())
        .createdAt(infrastructureDefinition.getCreatedAt())
        .environment(wingsPersistence.get(Environment.class, infrastructureDefinition.getEnvId()))
        .build();
  }
}
