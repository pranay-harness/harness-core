/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.environment;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;

import software.wings.beans.Environment;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLEnvironmentQueryParameters;
import software.wings.graphql.schema.type.QLEnvironment;
import software.wings.graphql.schema.type.QLEnvironment.QLEnvironmentBuilder;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class EnvironmentDataFetcher extends AbstractObjectDataFetcher<QLEnvironment, QLEnvironmentQueryParameters> {
  public static final String ENV_DOES_NOT_EXISTS_MSG = "Environment does not exist";

  @Inject HPersistence persistence;

  @Override
  @AuthRule(permissionType = PermissionType.ENV, action = Action.READ)
  public QLEnvironment fetch(QLEnvironmentQueryParameters qlQuery, String accountId) {
    Environment environment = persistence.get(Environment.class, qlQuery.getEnvironmentId());
    if (environment == null) {
      return null;
    }

    if (!environment.getAccountId().equals(accountId)) {
      throw new InvalidRequestException(ENV_DOES_NOT_EXISTS_MSG, WingsException.USER);
    }

    final QLEnvironmentBuilder builder = QLEnvironment.builder();
    EnvironmentController.populateEnvironment(environment, builder);
    return builder.build();
  }
}
