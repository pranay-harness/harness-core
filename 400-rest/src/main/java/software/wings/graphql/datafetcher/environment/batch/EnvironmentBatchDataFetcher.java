/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.environment.batch;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.graphql.datafetcher.AbstractBatchDataFetcher;
import software.wings.graphql.schema.query.QLEnvironmentQueryParameters;
import software.wings.graphql.schema.type.QLEnvironment;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import java.util.concurrent.CompletionStage;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dataloader.DataLoader;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class EnvironmentBatchDataFetcher
    extends AbstractBatchDataFetcher<QLEnvironment, QLEnvironmentQueryParameters, String> {
  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public CompletionStage<QLEnvironment> load(
      QLEnvironmentQueryParameters qlQuery, @NotNull DataLoader<String, QLEnvironment> dataLoader) {
    final String environmentId;
    if (StringUtils.isNotBlank(qlQuery.getEnvironmentId())) {
      environmentId = qlQuery.getEnvironmentId();
    } else {
      throw new InvalidRequestException("EnvironmentId not present in query", WingsException.USER);
    }
    return dataLoader.load(environmentId);
  }
}
