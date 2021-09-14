/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.graphql.datafetcher.environment;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.govern.Switch.unhandled;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EnvironmentType;

import software.wings.beans.Environment;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.QLEnvironment.QLEnvironmentBuilder;
import software.wings.graphql.schema.type.QLEnvironmentType;

import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class EnvironmentController {
  public static QLEnvironmentType convertEnvironmentType(EnvironmentType type) {
    switch (type) {
      case PROD:
        return QLEnvironmentType.PROD;
      case NON_PROD:
        return QLEnvironmentType.NON_PROD;
      default:
        unhandled(type);
    }
    return null;
  }

  public static void populateEnvironment(Environment environment, QLEnvironmentBuilder builder) {
    builder.id(environment.getUuid())
        .name(environment.getName())
        .description(environment.getDescription())
        .type(convertEnvironmentType(environment.getEnvironmentType()))
        .createdAt(environment.getCreatedAt())
        .createdBy(UserController.populateUser(environment.getCreatedBy()))
        .appId(environment.getAppId());
  }
}
