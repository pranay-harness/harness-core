package software.wings.api;

import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;

public class AwsAmiSetupExecutionDataTest extends CategoryTest {
  private AwsAmiSetupExecutionData awsAmiSetupExecutionData =
      AwsAmiSetupExecutionData.builder().maxInstances(2).desiredInstances(3).build();

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = awsAmiSetupExecutionData.getExecutionDetails();
    assertThat(executionDetails).isNotEmpty();
    assertThat(executionDetails).containsKey("desiredInstances");
    assertThat(executionDetails.get("desiredInstances").getDisplayName()).isEqualTo("Desired Instances");
    assertThat(executionDetails.get("desiredInstances").getValue()).isEqualTo(3);

    assertThat(executionDetails).containsKey("maxInstances");
    assertThat(executionDetails.get("maxInstances").getDisplayName()).isEqualTo("Max Instances");
    assertThat(executionDetails.get("maxInstances").getValue()).isEqualTo(2);
  }
}
