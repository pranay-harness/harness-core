package software.wings.sm.states.spotinst;

import static io.harness.rule.OwnerRule.SATYAM;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;

import io.harness.category.element.UnitTests;
import io.harness.delegate.task.spotinst.request.SpotInstDeployTaskParameters;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.api.ExecutionDataValue;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;

import java.util.Map;

public class SpotInstDeployStateExecutionDataTest extends WingsBaseTest {
  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testStateData() {
    String oldId = "oldId";
    String oldName = "foo__1";
    String newId = "newId";
    String newName = "foo__2";
    SpotInstDeployStateExecutionData stateData =
        SpotInstDeployStateExecutionData.builder()
            .oldElastiGroupId(oldId)
            .oldElastiGroupName(oldName)
            .oldDesiredCount(0)
            .newElastiGroupId(newId)
            .newElastiGroupName(newName)
            .newDesiredCount(2)
            .spotinstCommandRequest(
                SpotInstCommandRequest.builder()
                    .spotInstTaskParameters(SpotInstDeployTaskParameters.builder().activityId(ACTIVITY_ID).build())
                    .build())
            .build();
    Map<String, ExecutionDataValue> executionDetails = stateData.getExecutionDetails();
    assertThat(executionDetails.size()).isEqualTo(7);
    SpotinstDeployExecutionSummary stepExecutionSummary = stateData.getStepExecutionSummary();
    assertThat(stepExecutionSummary).isNotNull();
    assertThat(stepExecutionSummary.getOldElastigroupId()).isEqualTo(oldId);
    assertThat(stepExecutionSummary.getNewElastigroupId()).isEqualTo(newId);
  }
}