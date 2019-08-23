package migrations.all;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotEquals;

import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import io.harness.limits.ActionType;
import io.harness.limits.Counter;
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
  @Category(IntegrationTests.class)
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

    assertNotEquals("new entry(ies) should be created in `limitCounters` collection after migration", 0, finalCount);
  }
}
