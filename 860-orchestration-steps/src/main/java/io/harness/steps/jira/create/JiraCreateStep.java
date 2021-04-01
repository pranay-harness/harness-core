package io.harness.steps.jira.create;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.task.jira.JiraTaskNGParameters;
import io.harness.delegate.task.jira.JiraTaskNGParameters.JiraTaskNGParametersBuilder;
import io.harness.delegate.task.jira.JiraTaskNGResponse;
import io.harness.jira.JiraActionNG;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.EngineExceptionUtils;
import io.harness.pms.sdk.core.execution.ErrorDataException;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.jira.JiraIssueOutcome;
import io.harness.steps.jira.JiraTaskHelperService;

import com.google.inject.Inject;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@OwnedBy(CDC)
public class JiraCreateStep implements TaskExecutable<JiraCreateStepParameters, JiraTaskNGResponse> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(StepSpecTypeConstants.JIRA_CREATE).build();

  @Inject private JiraTaskHelperService jiraTaskHelperService;

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, JiraCreateStepParameters stepParameters, StepInputPackage inputPackage) {
    JiraTaskNGParametersBuilder paramsBuilder =
        JiraTaskNGParameters.builder()
            .action(JiraActionNG.CREATE_ISSUE)
            .projectKey(stepParameters.getProjectKey().getValue())
            .issueType(stepParameters.getIssueType().getValue())
            .fields(stepParameters.getFields() == null
                    ? null
                    : stepParameters.getFields().entrySet().stream().collect(
                        Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getValue())));
    return jiraTaskHelperService.prepareTaskRequest(paramsBuilder, ambiance,
        stepParameters.getConnectorRef().getValue(), stepParameters.getTimeout().getValue(), "Jira Task: Create Issue");
  }

  @Override
  public StepResponse handleTaskResult(
      Ambiance ambiance, JiraCreateStepParameters stepParameters, Supplier<JiraTaskNGResponse> responseSupplier) {
    StepResponseBuilder responseBuilder = StepResponse.builder();
    try {
      JiraTaskNGResponse taskResponse = responseSupplier.get();
      responseBuilder.status(Status.SUCCEEDED);
      responseBuilder.stepOutcome(StepResponse.StepOutcome.builder()
                                      .name("issue")
                                      .outcome(new JiraIssueOutcome(taskResponse.getIssue()))
                                      .build());
    } catch (ErrorDataException ex) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) ex.getErrorResponseData();
      responseBuilder.status(Status.FAILED);
      responseBuilder.failureInfo(FailureInfo.newBuilder()
                                      .setErrorMessage(errorNotifyResponseData.getErrorMessage())
                                      .addAllFailureTypes(EngineExceptionUtils.transformToOrchestrationFailureTypes(
                                          errorNotifyResponseData.getFailureTypes()))
                                      .build());
    }
    return responseBuilder.build();
  }

  @Override
  public Class<JiraCreateStepParameters> getStepParametersClass() {
    return JiraCreateStepParameters.class;
  }
}
