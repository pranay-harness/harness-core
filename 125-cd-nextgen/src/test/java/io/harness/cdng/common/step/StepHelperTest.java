package io.harness.cdng.common.step;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ACASIAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.environment.EnvironmentOutcome;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.contracts.refobjects.RefType;
import io.harness.pms.data.OrchestrationRefType;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.rule.Owner;

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
  @Mock private OutcomeService outcomeService;
  @InjectMocks private StepHelper stepHelper;
  private final Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", "test-account").build();

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldGetProdEnvType() {
    K8sDirectInfrastructureOutcome outcome =
        K8sDirectInfrastructureOutcome.builder()
            .environment(EnvironmentOutcome.builder().type(EnvironmentType.Production).build())
            .build();

    RefObject infra = RefObject.newBuilder()
                          .setName(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME)
                          .setKey(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME)
                          .setRefType(RefType.newBuilder().setType(OrchestrationRefType.OUTCOME).build())
                          .build();

    doReturn(outcome).when(outcomeService).resolve(ambiance, infra);
    io.harness.beans.EnvironmentType env = stepHelper.getEnvironmentType(ambiance);
    assertThat(env).isNotNull();
    assertThat(env).isEqualTo(io.harness.beans.EnvironmentType.PROD);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldGetNonProdEnvType() {
    K8sDirectInfrastructureOutcome outcome =
        K8sDirectInfrastructureOutcome.builder()
            .environment(EnvironmentOutcome.builder().type(EnvironmentType.PreProduction).build())
            .build();

    RefObject infra = RefObject.newBuilder()
                          .setName(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME)
                          .setKey(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME)
                          .setRefType(RefType.newBuilder().setType(OrchestrationRefType.OUTCOME).build())
                          .build();

    doReturn(outcome).when(outcomeService).resolve(ambiance, infra);
    io.harness.beans.EnvironmentType env = stepHelper.getEnvironmentType(ambiance);
    assertThat(env).isNotNull();
    assertThat(env).isEqualTo(io.harness.beans.EnvironmentType.NON_PROD);
  }
}
