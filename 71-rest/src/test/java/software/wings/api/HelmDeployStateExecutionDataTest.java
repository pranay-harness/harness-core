package software.wings.api;

import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.api.HelmDeployStateExecutionData.MAX_ERROR_MSG_LENGTH;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;

public class HelmDeployStateExecutionDataTest extends CategoryTest {
  private static final String errorMessage = "ErrorCase ";

  private HelmDeployStateExecutionData helmDeployStateExecutionData =
      HelmDeployStateExecutionData.builder().chartName("Chart_Name").build();

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetExecutionSummaryForMaxErrorMessageLength() {
    // Generate an errorMessage that is greater than HelmDeployStateExecutionData.MAX_ERROR_MSG_LENGTH

    StringBuilder errMsg = new StringBuilder();
    for (int i = 0; i < 1500; i++) {
      errMsg.append(errorMessage);
    }
    helmDeployStateExecutionData.setErrorMsg(errMsg.toString());

    Map<String, ExecutionDataValue> executionDetails = helmDeployStateExecutionData.getExecutionDetails();
    assertThat(executionDetails).isNotEmpty();
    assertThat(executionDetails).containsKey("errorMsg");
    assertThat(((String) executionDetails.get("errorMsg").getValue()).length()).isEqualTo(MAX_ERROR_MSG_LENGTH);
    assertThat(executionDetails).containsKey("chartName");
    assertThat((String) executionDetails.get("chartName").getValue()).isEqualTo("Chart_Name");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetExecutionSummaryForErrorMessageLength() {
    helmDeployStateExecutionData.setErrorMsg(errorMessage);

    Map<String, ExecutionDataValue> executionDetails = helmDeployStateExecutionData.getExecutionDetails();
    assertThat(executionDetails).isNotEmpty();
    assertThat(executionDetails).containsKey("errorMsg");
    assertThat(((String) executionDetails.get("errorMsg").getValue()).length()).isEqualTo(errorMessage.length());
    assertThat(executionDetails).containsKey("chartName");
    assertThat((String) executionDetails.get("chartName").getValue()).isEqualTo("Chart_Name");
  }
}
