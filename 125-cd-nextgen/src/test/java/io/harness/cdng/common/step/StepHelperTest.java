package io.harness.cdng.common.step;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.contracts.refobjects.RefType;
import io.harness.pms.data.OrchestrationRefType;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.rule.Owner;
import io.harness.steps.OutputExpressionConstants;
import io.harness.steps.StepHelper;
import io.harness.steps.environment.EnvironmentOutcome;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(CDP)
public class StepHelperTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @InjectMocks private StepHelper stepHelper;
  private final Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", "test-account").build();

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldGetProdEnvType() {
    EnvironmentOutcome environmentOutcome = EnvironmentOutcome.builder().type(EnvironmentType.Production).build();

    RefObject envRef = RefObject.newBuilder()
                           .setName(OutputExpressionConstants.ENVIRONMENT)
                           .setKey(OutputExpressionConstants.ENVIRONMENT)
                           .setRefType(RefType.newBuilder().setType(OrchestrationRefType.SWEEPING_OUTPUT).build())
                           .build();

    OptionalSweepingOutput optionalSweepingOutput =
        OptionalSweepingOutput.builder().found(true).output(environmentOutcome).build();

    doReturn(optionalSweepingOutput).when(executionSweepingOutputResolver).resolveOptional(ambiance, envRef);
    io.harness.beans.EnvironmentType env = stepHelper.getEnvironmentType(ambiance);
    assertThat(env).isNotNull();
    assertThat(env).isEqualTo(io.harness.beans.EnvironmentType.PROD);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldGetNonProdEnvType() {
    EnvironmentOutcome environmentOutcome = EnvironmentOutcome.builder().type(EnvironmentType.PreProduction).build();

    RefObject envRef = RefObject.newBuilder()
                           .setName(OutputExpressionConstants.ENVIRONMENT)
                           .setKey(OutputExpressionConstants.ENVIRONMENT)
                           .setRefType(RefType.newBuilder().setType(OrchestrationRefType.SWEEPING_OUTPUT).build())
                           .build();

    OptionalSweepingOutput optionalSweepingOutput =
        OptionalSweepingOutput.builder().found(true).output(environmentOutcome).build();

    doReturn(optionalSweepingOutput).when(executionSweepingOutputResolver).resolveOptional(ambiance, envRef);
    io.harness.beans.EnvironmentType env = stepHelper.getEnvironmentType(ambiance);
    assertThat(env).isNotNull();
    assertThat(env).isEqualTo(io.harness.beans.EnvironmentType.NON_PROD);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testShouldGetNullEnvType() {
    EnvironmentOutcome environmentOutcome = EnvironmentOutcome.builder().build();

    RefObject envRef = RefObject.newBuilder()
                           .setName(OutputExpressionConstants.ENVIRONMENT)
                           .setKey(OutputExpressionConstants.ENVIRONMENT)
                           .setRefType(RefType.newBuilder().setType(OrchestrationRefType.SWEEPING_OUTPUT).build())
                           .build();

    OptionalSweepingOutput optionalSweepingOutput =
        OptionalSweepingOutput.builder().found(true).output(environmentOutcome).build();

    doReturn(optionalSweepingOutput).when(executionSweepingOutputResolver).resolveOptional(ambiance, envRef);
    io.harness.beans.EnvironmentType env = stepHelper.getEnvironmentType(ambiance);
    assertThat(env).isNotNull();
    assertThat(env).isEqualTo(io.harness.beans.EnvironmentType.ALL);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetEnvTypeForNullEnvOutcome() {
    RefObject envRef = RefObject.newBuilder()
                           .setName(OutputExpressionConstants.ENVIRONMENT)
                           .setKey(OutputExpressionConstants.ENVIRONMENT)
                           .setRefType(RefType.newBuilder().setType(OrchestrationRefType.SWEEPING_OUTPUT).build())
                           .build();

    OptionalSweepingOutput optionalSweepingOutput = OptionalSweepingOutput.builder().found(false).build();

    doReturn(optionalSweepingOutput).when(executionSweepingOutputResolver).resolveOptional(ambiance, envRef);
    io.harness.beans.EnvironmentType env = stepHelper.getEnvironmentType(ambiance);
    assertThat(env).isNotNull();
    assertThat(env).isEqualTo(io.harness.beans.EnvironmentType.ALL);
  }
}
