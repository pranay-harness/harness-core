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
