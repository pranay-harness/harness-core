package software.wings.beans;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.serializer.JsonUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;

/**
 * Test case.
 *
 * @author Rishi
 */
public class ApplicationTest extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(ApplicationTest.class);
  @Inject private JsonUtils jsonUtils;

  /**
   * Test serialize deserialize.
   */
  @Test
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
