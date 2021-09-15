/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.graphql.datafetcher.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.Workflow;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.QLWorkflow.QLWorkflowBuilder;

import javax.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;

/**
 * Deliberately having a single class to adapt both
 * workflow and workflow execution.
 * Ideally, we should have two separate adapters.
 */
@OwnedBy(CDC)
@UtilityClass
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class WorkflowController {
  public static void populateWorkflow(@NotNull Workflow workflow, QLWorkflowBuilder builder) {
    builder.id(workflow.getUuid())
        .name(workflow.getName())
        .applicationId(workflow.getAppId())
        .description(workflow.getDescription())
        .createdAt(workflow.getCreatedAt())
        .createdBy(UserController.populateUser(workflow.getCreatedBy()));
  }
}
