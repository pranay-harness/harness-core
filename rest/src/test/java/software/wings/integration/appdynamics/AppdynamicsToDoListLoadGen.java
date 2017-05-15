package software.wings.integration.appdynamics;

import org.junit.Test;
import software.wings.integration.BaseIntegrationTest;

import javax.ws.rs.client.WebTarget;

/**
 * Created by rsingh on 5/15/17.
 */
public class AppdynamicsToDoListLoadGen extends BaseIntegrationTest {
  @Test
  public void generateLoad() throws InterruptedException {
    while (true) {
      try {
        WebTarget btTarget = client.target("http://localhost:8080/todolist/index.jsp");
        getRequestBuilder(btTarget).get();
        btTarget = client.target("http://localhost:8080/todolist/register");
        getRequestBuilder(btTarget).get();
        btTarget = client.target("http://localhost:8080/todolist/login");
        getRequestBuilder(btTarget).get();
        Thread.sleep(100);
      } catch (Throwable t) {
        System.out.println(t.fillInStackTrace());
      }
    }
  }
}
