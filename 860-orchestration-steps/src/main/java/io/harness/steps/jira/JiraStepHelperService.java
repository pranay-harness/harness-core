/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.steps.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.jira.JiraTaskNGParameters.JiraTaskNGParametersBuilder;
import io.harness.delegate.task.jira.JiraTaskNGResponse;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.supplier.ThrowingSupplier;

@OwnedBy(CDC)
public interface JiraStepHelperService {
  TaskRequest prepareTaskRequest(JiraTaskNGParametersBuilder paramsBuilder, Ambiance ambiance, String connectorRef,
      String timeStr, String taskName);
  StepResponse prepareStepResponse(ThrowingSupplier<JiraTaskNGResponse> responseSupplier) throws Exception;
}
