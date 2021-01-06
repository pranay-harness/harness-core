package io.harness.redesign.states.http.chain;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.TaskChainExecutable;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.redesign.states.http.BasicHttpStepParameters;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepUtils;
import io.harness.tasks.ResponseData;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

@OwnedBy(HarnessTeam.CDC)
public class BasicHttpChainStep implements TaskChainExecutable<BasicHttpChainStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType("HTTP_CHAIN").build();
  private static final int socketTimeoutMillis = 10000;

  @Inject private KryoSerializer kryoSerializer;

  @Override
  public Class<BasicHttpChainStepParameters> getStepParametersClass() {
    return BasicHttpChainStepParameters.class;
  }

  @Override
  public TaskChainResponse startChainLink(
      Ambiance ambiance, BasicHttpChainStepParameters stepParameters, StepInputPackage inputPackage) {
    BasicHttpChainStepParameters parameters = obtainBasicHttpChainStepParameters(stepParameters);
    BasicHttpStepParameters linkParam = parameters.getLinkParameters().get(0);
    TaskRequest taskRequest = buildTaskRequest(ambiance, linkParam);
    return TaskChainResponse.builder().chainEnd(false).taskRequest(taskRequest).build();
  }

  @Override
  public TaskChainResponse executeNextLink(Ambiance ambiance, BasicHttpChainStepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    BasicHttpChainStepParameters parameters = obtainBasicHttpChainStepParameters(stepParameters);
    BasicHttpStepParameters linkParam = parameters.getLinkParameters().get(1);
    TaskRequest taskRequest = buildTaskRequest(ambiance, linkParam);
    return TaskChainResponse.builder().chainEnd(true).taskRequest(taskRequest).build();
  }

  @Override
  public StepResponse finalizeExecution(Ambiance ambiance, BasicHttpChainStepParameters stepParameters,
      PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }

  private TaskRequest buildTaskRequest(Ambiance ambiance, BasicHttpStepParameters linkParam) {
    HttpTaskParameters httpTaskParameters = HttpTaskParameters.builder()
                                                .url(linkParam.getUrl())
                                                .body(linkParam.getBody())
                                                .header(linkParam.getHeader())
                                                .method(linkParam.getMethod())
                                                .socketTimeoutMillis(socketTimeoutMillis)
                                                .build();
    TaskData taskData = TaskData.builder()
                            .taskType(TaskType.HTTP.name())
                            .parameters(new Object[] {httpTaskParameters})
                            .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                            .build();

    return StepUtils.prepareTaskRequest(ambiance, taskData, kryoSerializer, new LinkedHashMap<>(),
        TaskCategory.DELEGATE_TASK_V1, Collections.emptyList());
  }

  @NotNull
  private BasicHttpChainStepParameters obtainBasicHttpChainStepParameters(BasicHttpChainStepParameters parameters) {
    if (isEmpty(parameters.getLinkParameters())) {
      throw new InvalidRequestException("No Chain Links Present");
    }
    return parameters;
  }
}
