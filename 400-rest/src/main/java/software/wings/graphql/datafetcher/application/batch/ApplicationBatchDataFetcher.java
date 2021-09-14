/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.application.batch;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.graphql.datafetcher.AbstractBatchDataFetcher;
import software.wings.graphql.schema.query.QLApplicationQueryParameters;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.WorkflowExecutionService;

import com.google.inject.Inject;
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
public class ApplicationBatchDataFetcher
    extends AbstractBatchDataFetcher<QLApplication, QLApplicationQueryParameters, String> {
  final WorkflowExecutionService workflowExecutionService;

  @Inject
  public ApplicationBatchDataFetcher(WorkflowExecutionService workflowExecutionService) {
    this.workflowExecutionService = workflowExecutionService;
  }

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public CompletionStage<QLApplication> load(
      QLApplicationQueryParameters qlQuery, @NotNull DataLoader<String, QLApplication> dataLoader) {
    final String applicationId;
    if (StringUtils.isNotBlank(qlQuery.getApplicationId())) {
      applicationId = qlQuery.getApplicationId();
    } else {
      throw new InvalidRequestException("ApplicationId not present in query", WingsException.USER);
    }
    return dataLoader.load(applicationId);
  }
}
