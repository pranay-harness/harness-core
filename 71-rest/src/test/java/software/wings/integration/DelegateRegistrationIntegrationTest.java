package software.wings.integration;

import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.ANUBHAW;
import static io.harness.rule.OwnerRule.PUNEET;
import static org.awaitility.Awaitility.await;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.rule.Owner;
import io.harness.rule.Repeat;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Duration;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.DelegateConnection;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 6/8/17.
 */
@Slf4j
public class DelegateRegistrationIntegrationTest extends BaseIntegrationTest {
  @Test
  @Owner(developers = ANUBHAW)
  @Repeat(times = 5, successes = 1)
  @Category(DeprecatedIntegrationTests.class)
  public void shouldWaitForADelegateToRegister() {
    // TODO: fix this method
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(DeprecatedIntegrationTests.class)
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
