package io.harness.states;

import static io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo.CALLBACK_IDS;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.ambiance.Ambiance;
import io.harness.beans.steps.CiStepOutcome;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.sweepingoutputs.StepTaskDetails;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.stepstatus.StepExecutionStatus;
import io.harness.delegate.task.stepstatus.StepMapOutput;
import io.harness.delegate.task.stepstatus.StepStatus;
import io.harness.delegate.task.stepstatus.StepStatusTaskResponseData;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.exception.FailureType;
import io.harness.execution.status.Status;
import io.harness.executionplan.CIExecutionTest;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.references.SweepingOutputRefObject;
import io.harness.rule.Owner;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.tasks.ResponseData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RunStepTest extends CIExecutionTest {
  public static final String STEP_ID = "runStepId";
  public static final String OUTPUT_KEY = "VAR1";
  public static final String OUTPUT_VALUE = "VALUE1";
  public static final String STEP_RESPONSE = "runStep";
  public static final String ERROR = "Error executing run step";
  @Mock private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @InjectMocks RunStep runStep;

  private Ambiance ambiance;
  private RunStepInfo stepInfo;
  private StepInputPackage stepInputPackage;
  private SweepingOutputRefObject refObject;
  private StepTaskDetails stepTaskDetails;
  private final String callbackId = UUID.randomUUID().toString();
  private Map<String, ResponseData> responseDataMap;

  @Before
  public void setUp() {
    ambiance = Ambiance.builder().build();
    stepInfo = RunStepInfo.builder().identifier(STEP_ID).build();
    stepInputPackage = StepInputPackage.builder().build();
    refObject = SweepingOutputRefObject.builder().name(CALLBACK_IDS).build();
    Map<String, String> callbackIds = new HashMap<>();
    callbackIds.put(STEP_ID, callbackId);
    stepTaskDetails = StepTaskDetails.builder().taskIds(callbackIds).build();
    responseDataMap = new HashMap<>();
  }

  @After
  public void tearDown() throws Exception {
    responseDataMap.clear();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldExecuteAsync() {
    when(executionSweepingOutputResolver.resolve(eq(ambiance), eq(refObject))).thenReturn(stepTaskDetails);
    AsyncExecutableResponse asyncExecutableResponse = runStep.executeAsync(ambiance, stepInfo, stepInputPackage);
    assertThat(asyncExecutableResponse).isEqualTo(AsyncExecutableResponse.builder().callbackId(callbackId).build());
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldHandleSuccessAsyncResponse() {
    responseDataMap.put(STEP_RESPONSE,
        StepStatusTaskResponseData.builder()
            .stepStatus(StepStatus.builder()
                            .stepExecutionStatus(StepExecutionStatus.SUCCESS)
                            .output(StepMapOutput.builder().output(OUTPUT_KEY, OUTPUT_VALUE).build())
                            .build())
            .build());
    StepResponse stepResponse = runStep.handleAsyncResponse(ambiance, stepInfo, responseDataMap);

    assertThat(stepResponse)
        .isEqualTo(
            StepResponse.builder()
                .status(Status.SUCCEEDED)
                .stepOutcome(StepResponse.StepOutcome.builder()
                                 .outcome(CiStepOutcome.builder()
                                              .output(StepMapOutput.builder().output(OUTPUT_KEY, OUTPUT_VALUE).build())
                                              .build())
                                 .name(STEP_ID)
                                 .build())
                .build());
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldHandleFailureAsyncResponse() {
    responseDataMap.put(STEP_RESPONSE,
        StepStatusTaskResponseData.builder()
            .stepStatus(StepStatus.builder().stepExecutionStatus(StepExecutionStatus.FAILURE).error(ERROR).build())
            .build());
    StepResponse stepResponse = runStep.handleAsyncResponse(ambiance, stepInfo, responseDataMap);

    assertThat(stepResponse)
        .isEqualTo(StepResponse.builder()
                       .status(Status.FAILED)
                       .failureInfo(FailureInfo.builder()
                                        .errorMessage(ERROR)
                                        .failureTypes(EnumSet.of(FailureType.APPLICATION_ERROR))
                                        .build())
                       .build());
  }
}