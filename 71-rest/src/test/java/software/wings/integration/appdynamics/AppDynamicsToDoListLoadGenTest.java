package software.wings.integration.appdynamics;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.integration.BaseIntegrationTest;

import javax.ws.rs.client.WebTarget;

/**
 * Created by rsingh on 5/15/17.
 */
public class AppDynamicsToDoListLoadGenTest extends BaseIntegrationTest {
  private final String baseUrl = "http://35.197.110.8/todolist";
  //  private final String baseUrl = "https://appd.cfapps.io";
  //  private final String baseUrl = "http://localhost:8080";
  @Test
  @Ignore
  public void generateLoadTest() throws InterruptedException {
    while (true) {
      try {
        WebTarget btTarget = client.target(baseUrl + "/exception");
        logger.info("" + getRequestBuilder(btTarget).get().getStatus());
        btTarget = client.target(baseUrl + "/register?name=cahksdc&password=abc&password2=abc&throwError=true");
        logger.info(getRequestBuilder(btTarget).post(null).toString());
        btTarget = client.target(baseUrl + "/login.jsp");
        logger.info("" + getRequestBuilder(btTarget).get().getStatus());
        btTarget = client.target(baseUrl + "/inside/load?priority=1&task=task1");
        logger.info("" + getRequestBuilder(btTarget).get().getStatus());
      } catch (Exception exception) {
        logger.info("", exception);
      }
    }
  }
}