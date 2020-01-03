package software.wings.beans;

import static io.harness.rule.OwnerRule.RAMA;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

/**
 * Test case.
 *
 * @author Rishi
 */
@Slf4j
public class ApplicationTest extends WingsBaseTest {
  @Inject private JsonUtils jsonUtils;

  /**
   * Test serialize deserialize.
   */
  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testSerializeDeserialize() {
    Application app = new Application();
    final String appName = "TestApp-" + System.currentTimeMillis();
    final String desc = "TestAppDesc-" + System.currentTimeMillis();
    app.setName(appName);
    app.setDescription(desc);
    app.onSave();

    // resetting createdBy and lastUpdatedBy since those fields are marked with @jsonIgnore
    app.setCreatedBy(null);
    app.setLastUpdatedBy(null);

    if (logger.isDebugEnabled()) {
      logger.debug("TestApp : " + app);
    }

    String json = jsonUtils.asJson(app);
    if (logger.isDebugEnabled()) {
      logger.debug("json : " + json);
    }

    Application app2 = jsonUtils.asObject(json, Application.class);
    assertThat(app2).isEqualToComparingFieldByField(app);
  }
}
