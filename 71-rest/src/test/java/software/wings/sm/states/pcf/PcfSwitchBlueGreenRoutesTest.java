package software.wings.sm.states.pcf;

import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.api.pcf.SetupSweepingOutputPcf;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;

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
}
