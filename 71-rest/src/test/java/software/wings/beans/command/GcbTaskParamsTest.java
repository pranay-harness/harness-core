package software.wings.beans.command;

import static io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability;
import static io.harness.rule.OwnerRule.VGLIJIN;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.rule.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import software.wings.beans.GcpConfig;

import java.util.List;

public class GcbTaskParamsTest extends CategoryTest {
  private static final GcpConfig gcpConfig = Mockito.mock(GcpConfig.class);
  private static final GcbTaskParams gcbTaskParam = GcbTaskParams.builder().gcpConfig(gcpConfig).build();

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void fetchRequiredExecutionCapabilitiesTest() {
    List<ExecutionCapability> capabilities = singletonList(buildHttpConnectionExecutionCapability("GCS_URL"));
    doReturn(capabilities).when(gcpConfig).fetchRequiredExecutionCapabilities();
    Assertions.assertThat(gcbTaskParam.fetchRequiredExecutionCapabilities()).isEqualTo(capabilities);
  }
}
