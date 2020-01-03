package software.wings.resources;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.AccountPlugin;
import software.wings.exception.ConstraintViolationExceptionMapper;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.PluginService;
import software.wings.utils.ResourceTestRule;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.GenericType;

/**
 * Created by peeyushaggarwal on 10/21/16.
 */
public class PluginResourceTest extends CategoryTest {
  public static final PluginService PLUGIN_SERVICE = mock(PluginService.class);

  @ClassRule
  public static final ResourceTestRule RESOURCES = ResourceTestRule.builder()
                                                       .addResource(new PluginResource(PLUGIN_SERVICE))
                                                       .addProvider(ConstraintViolationExceptionMapper.class)
                                                       .addProvider(WingsExceptionMapper.class)
                                                       .build();

  @Before
  public void setUp() throws IOException {
    reset(PLUGIN_SERVICE);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldGetInstalledPlugins() throws Exception {
    RestResponse<List<AccountPlugin>> restResponse = RESOURCES.client()
                                                         .target("/plugins/ACCOUNT_ID/installed")
                                                         .request()
                                                         .get(new GenericType<RestResponse<List<AccountPlugin>>>() {});
    verify(PLUGIN_SERVICE).getInstalledPlugins("ACCOUNT_ID");
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldGetInstalledPluginSettingSchema() throws Exception {
    RestResponse<Map<String, JsonNode>> restResponse =
        RESOURCES.client()
            .target("/plugins/ACCOUNT_ID/installed/settingschema")
            .request()
            .get(new GenericType<RestResponse<Map<String, JsonNode>>>() {});
    verify(PLUGIN_SERVICE).getPluginSettingSchema("ACCOUNT_ID");
  }
}
