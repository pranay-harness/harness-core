package io.harness.redesign.states.http.chain;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.ExcludeRedesign;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.TaskChainExecutable;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.redesign.states.http.BasicHttpStepParameters;
import io.harness.tasks.Cd1SetupFields;
import io.harness.tasks.ResponseData;

import software.wings.beans.TaskType;

import java.util.Map;
import org.jetbrains.annotations.NotNull;

@Redesign
@ExcludeRedesign
@OwnedBy(HarnessTeam.CDC)
public class BasicHttpChainStep implements TaskChainExecutable<BasicHttpChainStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType("HTTP_CHAIN").build();
  private static final int socketTimeoutMillis = 10000;

  @Override
  public Class<BasicHttpChainStepParameters> getStepParametersClass() {
    return BasicHttpChainStepParameters.class;
  }

  @Override
  public TaskChainResponse startChainLink(
      Ambiance ambiance, BasicHttpChainStepParameters stepParameters, StepInputPackage inputPackage) {
    BasicHttpChainStepParameters parameters = obtainBasicHttpChainStepParameters(stepParameters);
    BasicHttpStepParameters linkParam = parameters.getLinkParameters().get(0);
    DelegateTask task = buildTask(ambiance, linkParam);
    return TaskChainResponse.builder().chainEnd(false).task(task).build();
  }

  @Override
  public TaskChainResponse executeNextLink(Ambiance ambiance, BasicHttpChainStepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    BasicHttpChainStepParameters parameters = obtainBasicHttpChainStepParameters(stepParameters);
    BasicHttpStepParameters linkParam = parameters.getLinkParameters().get(1);
    DelegateTask task = buildTask(ambiance, linkParam);
    return TaskChainResponse.builder().chainEnd(true).task(task).build();
  }

  @Override
  public StepResponse finalizeExecution(Ambiance ambiance, BasicHttpChainStepParameters stepParameters,
      PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }

  private DelegateTask buildTask(Ambiance ambiance, BasicHttpStepParameters linkParam) {
    HttpTaskParameters httpTaskParameters = HttpTaskParameters.builder()
                                                .url(linkParam.getUrl())
                                                .body(linkParam.getBody())
                                                .header(linkParam.getHeader())
                                                .method(linkParam.getMethod())
                                                .socketTimeoutMillis(socketTimeoutMillis)
                                                .build();

    String waitId = generateUuid();
    return DelegateTask.builder()
        .accountId(ambiance.getSetupAbstractionsMap().get("accountId"))
        .waitId(waitId)
        .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, ambiance.getSetupAbstractionsMap().get("appId"))
        .data(TaskData.builder()
                  .taskType(TaskType.HTTP.name())
                  .parameters(new Object[] {httpTaskParameters})
                  .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                  .build())
        .setupAbstraction(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD, waitId)
        .build();
  }

  @NotNull
  private BasicHttpChainStepParameters obtainBasicHttpChainStepParameters(BasicHttpChainStepParameters parameters) {
    if (isEmpty(parameters.getLinkParameters())) {
      throw new InvalidRequestException("No Chain Links Present");
    }
    return parameters;
  }
}
