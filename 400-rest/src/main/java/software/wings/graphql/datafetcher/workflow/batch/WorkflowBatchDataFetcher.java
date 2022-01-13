/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.workflow.batch;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.graphql.datafetcher.AbstractBatchDataFetcher;
import software.wings.graphql.schema.query.QLWorkflowQueryParameters;
import software.wings.graphql.schema.type.QLWorkflow;
import software.wings.security.annotations.AuthRule;

import java.util.concurrent.CompletionStage;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dataloader.DataLoader;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class WorkflowBatchDataFetcher extends AbstractBatchDataFetcher<QLWorkflow, QLWorkflowQueryParameters, String> {
  @Override
  @AuthRule(permissionType = LOGGED_IN)
  protected CompletionStage<QLWorkflow> load(
      QLWorkflowQueryParameters parameters, DataLoader<String, QLWorkflow> dataLoader) {
    final String workflowId;
    if (StringUtils.isNotBlank(parameters.getWorkflowId())) {
      workflowId = parameters.getWorkflowId();
    } else {
      throw new InvalidRequestException("Workflow Id not present in query", WingsException.USER);
    }
    return dataLoader.load(workflowId);
  }
}
