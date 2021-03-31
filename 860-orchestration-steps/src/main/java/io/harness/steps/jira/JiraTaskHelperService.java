package io.harness.steps.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.jira.JiraTaskNGParameters.JiraTaskNGParametersBuilder;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;

@OwnedBy(CDC)
public interface JiraTaskHelperService {
  TaskRequest prepareTaskRequest(JiraTaskNGParametersBuilder paramsBuilder, Ambiance ambiance, String connectorRef,
      String timeStr, String taskName);
}
