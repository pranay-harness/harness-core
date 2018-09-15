package software.wings.integration;

import static io.harness.persistence.HQuery.excludeAuthority;
import static org.awaitility.Awaitility.await;

import org.awaitility.Duration;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateConnection;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 6/8/17.
 */
public class DelegateRegistrationIntegrationTest extends BaseIntegrationTest {
  @Test
  public void shouldWaitForADelegateToRegister() {
    await().with().pollInterval(Duration.ONE_SECOND).timeout(5, TimeUnit.MINUTES).until(() -> {
      List<Delegate> delegates = wingsPersistence.createQuery(Delegate.class, excludeAuthority).asList();
      boolean connected = delegates.stream().anyMatch(Delegate::isConnected);
      logger.info("isDelegateConnected = {}", connected);
      return connected;
    }, CoreMatchers.is(true));
  }

  @Test
  public void shouldWaitForADelegateConnectionsToAppear() {
    await().with().pollInterval(Duration.ONE_SECOND).timeout(5, TimeUnit.MINUTES).until(() -> {
      List<DelegateConnection> delegateConnections =
          wingsPersistence.createQuery(DelegateConnection.class, excludeAuthority).asList();
      boolean connected = !delegateConnections.isEmpty();
      logger.info("Got {} delegate connections.", delegateConnections.size());
      return connected;
    }, CoreMatchers.is(true));
  }
}
