package software.wings.sm.states.pcf;

import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.TMACARI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.api.pcf.SetupSweepingOutputPcf;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.sm.ExecutionContextImpl;

public class PcfSwitchBlueGreenRoutesTest extends CategoryTest {
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetNewApplicationName() {
    PcfSwitchBlueGreenRoutes pcfSwitchBlueGreenRoutes = new PcfSwitchBlueGreenRoutes("");
    assertThat(pcfSwitchBlueGreenRoutes.getNewApplicationName(null)).isEqualTo(StringUtils.EMPTY);
    assertThat(pcfSwitchBlueGreenRoutes.getNewApplicationName(SetupSweepingOutputPcf.builder().build()))
        .isEqualTo(StringUtils.EMPTY);
    assertThat(pcfSwitchBlueGreenRoutes.getNewApplicationName(
                   SetupSweepingOutputPcf.builder()
                       .newPcfApplicationDetails(PcfAppSetupTimeDetails.builder().applicationName("name").build())
                       .build()))
        .isEqualTo("name");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetTimeoutMillis() throws IllegalAccessException {
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    PcfSwitchBlueGreenRoutes pcfSwitchBlueGreenRoutes = new PcfSwitchBlueGreenRoutes("name");
    PcfStateHelper mockPcfStateHelper = mock(PcfStateHelper.class);
    FieldUtils.writeField(pcfSwitchBlueGreenRoutes, "pcfStateHelper", mockPcfStateHelper, true);
    doReturn(10).when(mockPcfStateHelper).getStateTimeoutMillis(context, 5, false);
    assertThat(pcfSwitchBlueGreenRoutes.getTimeoutMillis(context)).isEqualTo(10);
  }
}
