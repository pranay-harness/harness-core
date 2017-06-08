package software.wings.integration.appdynamics;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.integration.BaseIntegrationTest;

import javax.ws.rs.client.WebTarget;

/**
 * Created by rsingh on 5/15/17.
 */
public class AppDynamicsToDoListLoadGenTest extends BaseIntegrationTest {
  // private final String baseUrl = "http://rsingh-test-1026806332.us-east-1.elb.amazonaws.com";
  //    private final String baseUrl = "http://35.185.240.190";
  private final String baseUrl = "http://localhost:8080";
  @Test
  @Ignore
  public void generateLoadTest() throws InterruptedException {
    while (true) {
      try {
        WebTarget btTarget = client.target(baseUrl + "/todolist/index.jsp");
        getRequestBuilder(btTarget).get();
        btTarget = client.target(baseUrl + "/todolist");
        getRequestBuilder(btTarget).get();
        btTarget = client.target(baseUrl + "/todolist/register");
        getRequestBuilder(btTarget).get();
        btTarget = client.target(baseUrl + "/todolist/login");
        getRequestBuilder(btTarget).get();
      } catch (Throwable t) {
        System.out.println(t.fillInStackTrace());
      }
    }
  }
}