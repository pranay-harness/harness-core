/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.graphql.datafetcher.execution;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.pipeline.input.QLRuntimeExecutionInputs;
import software.wings.graphql.schema.mutation.pipeline.payload.QLContinueExecutionPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class PipelineResumeRuntimeDataFetcher
    extends BaseMutatorDataFetcher<QLRuntimeExecutionInputs, QLContinueExecutionPayload> {
  @Inject ResumePipelineController resumePipelineController;
  public PipelineResumeRuntimeDataFetcher() {
    super(QLRuntimeExecutionInputs.class, QLContinueExecutionPayload.class);
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLContinueExecutionPayload mutateAndFetch(
      QLRuntimeExecutionInputs parameter, MutationContext mutationContext) {
    return resumePipelineController.resumePipeline(parameter);
  }
}
