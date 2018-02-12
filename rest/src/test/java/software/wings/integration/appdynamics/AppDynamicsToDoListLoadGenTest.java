package software.wings.integration.appdynamics;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.integration.BaseIntegrationTest;

import javax.ws.rs.client.WebTarget;

/**
 * Created by rsingh on 5/15/17.
 */
public class AppDynamicsToDoListLoadGenTest extends BaseIntegrationTest {
  //  private final String baseUrl = "http://rsingh-test-1026806332.us-east-1.elb.amazonaws.com";
  private final String baseUrl = "http://35.192.85.162";
  //  private final String baseUrl = "http://localhost:8080";
  @Test
  @Ignore
  public void generateLoadTest() throws InterruptedException {
    while (true) {
      try {
        WebTarget btTarget = client.target(baseUrl + "/todolist/exception");
        getRequestBuilder(btTarget).get();
        btTarget = client.target(baseUrl + "/todolist/register?name=cahksdc&password=abc&password2=abc");
        System.out.println(getRequestBuilder(btTarget).post(null));
        btTarget = client.target(baseUrl + "/todolist/login.jsp");
        System.out.println(getRequestBuilder(btTarget).get().getStatus());
        btTarget = client.target(baseUrl + "/todolist/inside/load?priority=1&task=task1");
        System.out.println(getRequestBuilder(btTarget).get().getStatus());
      } catch (Throwable t) {
        System.out.println(t.fillInStackTrace());
      }
    }
  }
}