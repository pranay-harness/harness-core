package io.harness.cvng.cdng.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.cdng.beans.CVNGStepParameter;
import io.harness.cvng.cdng.beans.TestVerificationJobSpec;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CVNGStepTest extends CvNextGenTestBase {
  private CVNGStep cvngStep;
  @Inject private Injector injector;

  @Before
  public void setup() {
    cvngStep = new CVNGStep();
    injector.injectMembers(cvngStep);
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecuteAsync_noJobExistException() {
    String uuid = generateUuid();
    HashMap<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put("accountId", "accountId");
    setupAbstractions.put("projectIdentifier", "projectId");
    setupAbstractions.put("orgIdentifier", "orgId");
    Ambiance ambiance = Ambiance.newBuilder()
                            .addAllLevels(Collections.singletonList(Level.newBuilder().setRuntimeId(uuid).build()))
                            .setPlanExecutionId(generateUuid())
                            .putAllSetupAbstractions(setupAbstractions)
                            .build();
    StepInputPackage stepInputPackage = StepInputPackage.builder().build();
    TestVerificationJobSpec spec = TestVerificationJobSpec.builder()
                                       .deploymentTag(randomParameter())
                                       .serviceRef(randomParameter())
                                       .envRef(randomParameter())
                                       .duration(ParameterField.<String>builder().value("5m").build())
                                       .sensitivity(ParameterField.<String>builder().value("High").build())
                                       .build();
    String verificationJobIdentifier = "testJob";
    CVNGStepParameter cvngStepParameter = CVNGStepParameter.builder()
                                              .verificationJobIdentifier(verificationJobIdentifier)
                                              .serviceIdentifier(spec.getServiceRef())
                                              .envIdentifier(spec.getEnvRef())
                                              .deploymentTag(spec.getDeploymentTag())
                                              .runtimeValues(spec.getRuntimeValues())
                                              .build();

    assertThatThrownBy(() -> cvngStep.executeAsync(ambiance, cvngStepParameter, stepInputPackage))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("No Job exists for verificationJobIdentifier: 'testJob'");
  }
  // TODO: more tests..
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    String uuid = generateUuid();
    HashMap<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put("accountId", "accountId");
    setupAbstractions.put("projectIdentifier", "projectId");
    setupAbstractions.put("orgIdentifier", "orgId");
    Ambiance ambiance = Ambiance.newBuilder()
                            .addAllLevels(Collections.singletonList(Level.newBuilder().setRuntimeId(uuid).build()))
                            .setPlanExecutionId(generateUuid())
                            .putAllSetupAbstractions(setupAbstractions)
                            .build();
    TestVerificationJobSpec spec = TestVerificationJobSpec.builder()
                                       .deploymentTag(randomParameter())
                                       .serviceRef(randomParameter())
                                       .envRef(randomParameter())
                                       .duration(ParameterField.<String>builder().value("5m").build())
                                       .sensitivity(ParameterField.<String>builder().value("High").build())
                                       .build();
    String verificationJobIdentifier = "testJob";
    CVNGStepParameter cvngStepParameter = CVNGStepParameter.builder()
                                              .verificationJobIdentifier(verificationJobIdentifier)
                                              .serviceIdentifier(spec.getServiceRef())
                                              .envIdentifier(spec.getEnvRef())
                                              .deploymentTag(spec.getDeploymentTag())
                                              .runtimeValues(spec.getRuntimeValues())
                                              .build();
    String activityId = generateUuid();
    ActivityStatusDTO activityStatusDTO = ActivityStatusDTO.builder()
                                              .activityId(activityId)
                                              .status(ActivityVerificationStatus.VERIFICATION_PASSED)
                                              .progressPercentage(100)
                                              .durationMs(Duration.ofMinutes(30).toMillis())
                                              .build();
    StepResponse stepResponse = cvngStep.handleAsyncResponse(ambiance, cvngStepParameter,
        Collections.singletonMap(activityId,
            CVNGStep.CVNGResponseData.builder().activityId(activityId).activityStatusDTO(activityStatusDTO).build()));
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
    assertThat(stepResponse.getFailureInfo()).isNull();
    StepOutcome stepOutcome =
        StepOutcome.builder()
            .name("output")
            .outcome(CVNGStep.VerifyStepOutcome.builder().activityStatus(activityStatusDTO).build())
            .build();
    assertThat(stepResponse.getStepOutcomes()).isEqualTo(Collections.singletonList(stepOutcome));
  }
  // TODO: more tests..

  private ParameterField<String> randomParameter() {
    return ParameterField.<String>builder().value(generateUuid()).build();
  }
}