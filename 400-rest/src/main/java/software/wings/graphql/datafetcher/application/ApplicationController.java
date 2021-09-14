/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.application;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.Application;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.QLApplication.QLApplicationBuilder;

import lombok.experimental.UtilityClass;

@UtilityClass
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class ApplicationController {
  public static QLApplicationBuilder populateQLApplication(Application application, QLApplicationBuilder builder) {
    return builder.id(application.getAppId())
        .name(application.getName())
        .description(application.getDescription())
        .createdAt(application.getCreatedAt())
        .createdBy(UserController.populateUser(application.getCreatedBy()))
        .isManualTriggerAuthorized(application.getIsManualTriggerAuthorized());
  }
}
