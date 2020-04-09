package software.wings.beans.settings.helm;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.List;
import java.util.stream.Collectors;

public class GCSHelmRepoConfigTest extends WingsBaseTest {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void fetchRequiredExecutionCapabilities() {
    GCSHelmRepoConfig repoConfig = GCSHelmRepoConfig.builder().build();
    List<ExecutionCapability> executionCapabilityList = repoConfig.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilityList).isNotEmpty();
    assertThat(executionCapabilityList).hasSize(2);
    assertThat(
        executionCapabilityList.stream().map(ExecutionCapability::getCapabilityType).collect(Collectors.toList()))
        .containsExactlyInAnyOrder(CapabilityType.HELM_INSTALL, CapabilityType.CHART_MUSEUM);
  }
}