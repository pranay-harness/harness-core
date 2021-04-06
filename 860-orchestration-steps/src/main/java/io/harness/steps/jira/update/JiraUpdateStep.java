package io.harness.steps.jira.update;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.jira.JiraTaskNGParameters;
import io.harness.delegate.task.jira.JiraTaskNGParameters.JiraTaskNGParametersBuilder;
import io.harness.delegate.task.jira.JiraTaskNGResponse;
import io.harness.jira.JiraActionNG;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.jira.JiraStepHelperService;

import com.google.inject.Inject;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@OwnedBy(CDC)
public class JiraUpdateStep implements TaskExecutable<JiraUpdateStepParameters, JiraTaskNGResponse> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(StepSpecTypeConstants.JIRA_UPDATE).build();

  @Inject private JiraStepHelperService jiraStepHelperService;

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, JiraUpdateStepParameters stepParameters, StepInputPackage inputPackage) {
    JiraTaskNGParametersBuilder paramsBuilder =
        JiraTaskNGParameters.builder()
            .action(JiraActionNG.UPDATE_ISSUE)
            .issueKey(stepParameters.getIssueKey().getValue())
            .transitionToStatus(stepParameters.getTransitionTo() == null
                    ? null
                    : (String) stepParameters.getTransitionTo().getStatus().fetchFinalValue())
            .transitionName(stepParameters.getTransitionTo() == null
                    ? null
                    : (String) stepParameters.getTransitionTo().getTransitionName().fetchFinalValue())
            .fields(stepParameters.getFields() == null
                    ? null
                    : stepParameters.getFields().entrySet().stream().collect(
                        Collectors.toMap(Map.Entry::getKey, e -> (String) e.getValue().fetchFinalValue())));
    return jiraStepHelperService.prepareTaskRequest(paramsBuilder, ambiance,
        stepParameters.getConnectorRef().getValue(), stepParameters.getTimeout().getValue(), "Jira Task: Update Issue");
  }

  @Override
  public StepResponse handleTaskResult(
      Ambiance ambiance, JiraUpdateStepParameters stepParameters, Supplier<JiraTaskNGResponse> responseSupplier) {
    return jiraStepHelperService.prepareStepResponse(responseSupplier);
  }

  @Override
  public Class<JiraUpdateStepParameters> getStepParametersClass() {
    return JiraUpdateStepParameters.class;
  }
}
