package software.wings.api.pcf;

import static io.harness.pcf.model.PcfConstants.CONTEXT_APP_FINAL_ROUTES;
import static io.harness.pcf.model.PcfConstants.CONTEXT_APP_TEMP_ROUTES;
import static io.harness.pcf.model.PcfConstants.CONTEXT_NEW_APP_GUID;
import static io.harness.pcf.model.PcfConstants.CONTEXT_NEW_APP_NAME;
import static io.harness.pcf.model.PcfConstants.CONTEXT_NEW_APP_ROUTES;
import static io.harness.pcf.model.PcfConstants.CONTEXT_OLD_APP_GUID;
import static io.harness.pcf.model.PcfConstants.CONTEXT_OLD_APP_NAME;
import static io.harness.pcf.model.PcfConstants.CONTEXT_OLD_APP_ROUTES;
import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PcfSetupContextElementTest extends CategoryTest {
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void shouldGetExecutionSummary() {
    String guid = "1234_0";
    String name = "appName_0";

    String guid1 = "1234_1";
    String name1 = "appName_1";

    List<String> tempRoutes = Arrays.asList("r1", "r2");
    List<String> finalRoutes = Arrays.asList("r3", "r4");
    PcfSetupContextElement pcfSetupContextElement = new PcfSetupContextElement();
    pcfSetupContextElement.setNewPcfApplicationDetails(
        PcfAppSetupTimeDetails.builder().applicationGuid(guid1).applicationName(name1).urls(tempRoutes).build());

    pcfSetupContextElement.setAppDetailsToBeDownsized(Arrays.asList(
        PcfAppSetupTimeDetails.builder().applicationGuid(guid).applicationName(name).urls(finalRoutes).build()));

    pcfSetupContextElement.setRouteMaps(finalRoutes);
    pcfSetupContextElement.setTempRouteMap(tempRoutes);

    Map<String, Object> map = pcfSetupContextElement.paramMap(null);
    assertThat(map).isNotEmpty();
    assertThat(map.containsKey("pcf")).isTrue();
    Map<String, Object> paramMap = (Map<String, Object>) map.get("pcf");

    assertThat(paramMap).isNotEmpty();
    assertThat(paramMap.get(CONTEXT_NEW_APP_NAME)).isEqualTo(name1);
    assertThat(paramMap.get(CONTEXT_NEW_APP_GUID)).isEqualTo(guid1);
    assertThat(paramMap.get(CONTEXT_NEW_APP_ROUTES)).isEqualTo(tempRoutes);

    assertThat(paramMap.get(CONTEXT_OLD_APP_NAME)).isEqualTo(name);
    assertThat(paramMap.get(CONTEXT_OLD_APP_GUID)).isEqualTo(guid);
    assertThat(paramMap.get(CONTEXT_OLD_APP_ROUTES)).isEqualTo(finalRoutes);

    assertThat(paramMap.get(CONTEXT_APP_TEMP_ROUTES)).isEqualTo(tempRoutes);
    assertThat(paramMap.get(CONTEXT_APP_FINAL_ROUTES)).isEqualTo(finalRoutes);
  }
}
