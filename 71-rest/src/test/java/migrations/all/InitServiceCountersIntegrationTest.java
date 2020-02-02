package migrations.all;

import static io.harness.rule.OwnerRule.ANKIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.limits.ActionType;
import io.harness.limits.Counter;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Service;
import software.wings.integration.BaseIntegrationTest;

public class InitServiceCountersIntegrationTest extends BaseIntegrationTest {
  @Inject private InitServiceCounters initServiceCounters;

  @Before
  public void init() {
    wingsPersistence.delete(
        wingsPersistence.createQuery(Counter.class).field("key").endsWith(ActionType.CREATE_SERVICE.toString()));
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(DeprecatedIntegrationTests.class)
  public void testMigrate() {
    long serviceCount = wingsPersistence.createQuery(Service.class).count();
    if (serviceCount == 0) {
      return;
    }

    long initialCount =
        wingsPersistence.createQuery(Counter.class).field("key").endsWith(ActionType.CREATE_SERVICE.toString()).count();

    assertThat(initialCount).isEqualTo(0);
    initServiceCounters.migrate();

    long finalCount =
        wingsPersistence.createQuery(Counter.class).field("key").endsWith(ActionType.CREATE_SERVICE.toString()).count();

    assertThat(0).isNotEqualTo(finalCount);
  }
}
