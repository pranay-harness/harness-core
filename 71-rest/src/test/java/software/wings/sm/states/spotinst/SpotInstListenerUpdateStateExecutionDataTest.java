package software.wings.sm.states.spotinst;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;

import io.harness.category.element.UnitTests;
import io.harness.delegate.task.aws.LoadBalancerDetailsForBGDeployment;
import io.harness.delegate.task.spotinst.request.SpotInstSwapRoutesTaskParameters;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.api.ExecutionDataValue;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;

import java.util.Map;

public class SpotInstListenerUpdateStateExecutionDataTest extends WingsBaseTest {
  @Test
  @Category(UnitTests.class)
  public void testStateData() {
    String oldId = "oldId";
    String oldName = "foo";
    String newId = "newId";
    String newName = "foo__STAGE__Harness";
    SpotInstListenerUpdateStateExecutionData data =
        SpotInstListenerUpdateStateExecutionData.builder()
            .oldElastiGroupId(oldId)
            .oldElastiGroupName(oldName)
            .newElastiGroupId(newId)
            .newElastiGroupName(newName)
            .spotinstCommandRequest(
                SpotInstCommandRequest.builder()
                    .spotInstTaskParameters(SpotInstSwapRoutesTaskParameters.builder().activityId(ACTIVITY_ID).build())
                    .build())
            .isRollback(false)
            .lbDetails(singletonList(LoadBalancerDetailsForBGDeployment.builder()
                                         .loadBalancerName("lb-name")
                                         .prodListenerArn("prod-listener-arn")
                                         .prodListenerPort("8080")
                                         .prodTargetGroupArn("prod-tgt-arn")
                                         .prodTargetGroupName("prod-tgt-name")
                                         .stageListenerArn("stage-listener-arn")
                                         .stageListenerPort("8181")
                                         .stageTargetGroupArn("stage-tgt-arn")
                                         .stageTargetGroupName("stage-tgt-name")
                                         .build()))
            .downsizeOldElastiGroup(true)
            .build();
    Map<String, ExecutionDataValue> executionSummary = data.getExecutionSummary();
    assertThat(executionSummary.size()).isEqualTo(4);
    SpotinstDeployExecutionSummary stepExecutionSummary = data.getStepExecutionSummary();
    assertThat(stepExecutionSummary).isNotNull();
    assertThat(stepExecutionSummary.getOldElastigroupId()).isEqualTo(oldId);
    assertThat(stepExecutionSummary.getNewElastigroupId()).isEqualTo(newId);
  }
}