package software.wings.beans;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.WingsBaseTest;
import software.wings.utils.JsonUtils;

import javax.inject.Inject;

/**
 * Test case
 *
 * @author Rishi
 */
public class ApplicationTest extends WingsBaseTest {
  @Inject private JsonUtils jsonUtils;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Test
  public void testSerializeDeserialize() {
    Application app = new Application();
    final String appName = "TestApp-" + System.currentTimeMillis();
    final String desc = "TestAppDesc-" + System.currentTimeMillis();
    app.setName(appName);
    app.setDescription(desc);
    app.onSave();
    logger.debug("TestApp : " + app);

    String json = jsonUtils.asJson(app);
    logger.debug("json : " + json);

    Application app2 = jsonUtils.asObject(json, Application.class);
    assertThat(app2).isEqualToComparingFieldByField(app);
  }
}
