package io.harness.stateutils.buildstate;

import static io.harness.rule.OwnerRule.SHUBHAM;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.beans.steps.stepinfo.InitializeStepInfo;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.StageDetails;
import io.harness.beans.yaml.extended.infrastrucutre.AwsVmInfraYaml;
import io.harness.beans.yaml.extended.infrastrucutre.AwsVmInfraYaml.AwsVmInfraYamlSpec;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ci.awsvm.CIAWSVmInitializeTaskParams;
import io.harness.executionplan.CIExecutionTestBase;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.rule.Owner;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AWSVmInitializeTaskUtilsTest extends CIExecutionTestBase {
  @Mock private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @InjectMocks private AWSVmInitializeTaskUtils awsVmInitializeTaskUtils;
  private Ambiance ambiance;

  @Before
  public void setUp() {
    Map<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put("accountId", "accountId");
    ambiance = Ambiance.newBuilder().putAllSetupAbstractions(setupAbstractions).build();
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getInitializeTaskParams() {
    String poolId = "test";
    String stageRuntimeId = "test";
    InitializeStepInfo initializeStepInfo =
        InitializeStepInfo.builder()
            .infrastructure(AwsVmInfraYaml.builder().spec(AwsVmInfraYamlSpec.builder().poolId(poolId).build()).build())
            .build();
    when(executionSweepingOutputResolver.consume(any(), any(), any(), any())).thenReturn("");
    when(executionSweepingOutputResolver.resolveOptional(
             ambiance, RefObjectUtils.getSweepingOutputRefObject(ContextElement.stageDetails)))
        .thenReturn(OptionalSweepingOutput.builder()
                        .found(true)
                        .output(StageDetails.builder().stageRuntimeID(stageRuntimeId).build())
                        .build());
    CIAWSVmInitializeTaskParams response =
        awsVmInitializeTaskUtils.getInitializeTaskParams(initializeStepInfo, ambiance, "");
    assertThat(response.getStageRuntimeId()).isEqualTo(stageRuntimeId);
  }
}