package software.wings.api.pcf;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;

public class PcfSetupStateExecutionDataTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void shouldGetExecutionSummary() {
    String org = "org";
    String space = "space";
    PcfSetupStateExecutionData pcfSetupStateExecutionData = new PcfSetupStateExecutionData();
    PcfSetupExecutionSummary stepExecutionSummary = pcfSetupStateExecutionData.getStepExecutionSummary();
    assertThat(stepExecutionSummary).isNotNull();
    assertThat(stepExecutionSummary.getMaxInstanceCount()).isEqualTo(0);

    pcfSetupStateExecutionData.setUseCurrentRunningInstanceCount(false);
    stepExecutionSummary = pcfSetupStateExecutionData.getStepExecutionSummary();
    assertThat(stepExecutionSummary).isNotNull();
    assertThat(stepExecutionSummary.getMaxInstanceCount()).isEqualTo(0);

    pcfSetupStateExecutionData.setMaxInstanceCount(2);
    pcfSetupStateExecutionData.setPcfCommandRequest(
        PcfCommandSetupRequest.builder().organization(org).space(space).build());
    stepExecutionSummary = pcfSetupStateExecutionData.getStepExecutionSummary();
    assertThat(stepExecutionSummary).isNotNull();
    assertThat(stepExecutionSummary.getMaxInstanceCount()).isEqualTo(2);
    assertThat(stepExecutionSummary.getOrganization()).isEqualTo(org);
    assertThat(stepExecutionSummary.getSpace()).isEqualTo(space);

    pcfSetupStateExecutionData.setUseCurrentRunningInstanceCount(true);
    pcfSetupStateExecutionData.setCurrentRunningInstanceCount(1);
    assertThat(stepExecutionSummary).isNotNull();
    stepExecutionSummary = pcfSetupStateExecutionData.getStepExecutionSummary();
    assertThat(stepExecutionSummary.getMaxInstanceCount()).isEqualTo(1);
    assertThat(stepExecutionSummary.getOrganization()).isEqualTo(org);
    assertThat(stepExecutionSummary.getSpace()).isEqualTo(space);
  }
}
