
package software.wings.beans;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.wings.common.JsonUtils;
/**
 * Test case
 * @author Rishi
 *
 */
public class ApplicationTest {
  @Test
  public void testSerializeDeserialize() {
    Application app = new Application();
    final String appName = "TestApp-" + System.currentTimeMillis();
    final String desc = "TestAppDesc-" + System.currentTimeMillis();
    app.setName(appName);
    app.setDescription(desc);
    app.onUpdate();
    logger.debug("TestApp : " + app);

    String json = JsonUtils.asJson(app);
    logger.debug("json : " + json);

    Application app2 = JsonUtils.asObject(json, Application.class);
    assertThat(app2).isEqualToComparingFieldByField(app);
  }

  private static Logger logger = LoggerFactory.getLogger(ApplicationTest.class);
}
