package migrations.all;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import io.harness.limits.ActionType;
import io.harness.limits.Counter;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Pipeline;
import software.wings.integration.BaseIntegrationTest;

public class InitPipelineCountersIntegrationTest extends BaseIntegrationTest {
  @Inject private InitPipelineCounters initPipelineCounters;

  @Before
  public void init() {
    wingsPersistence.delete(
        wingsPersistence.createQuery(Counter.class).field("key").endsWith(ActionType.CREATE_PIPELINE.toString()));
  }

  @Test
  @Category(IntegrationTests.class)
  public void testMigrate() {
    long pipelineCount = wingsPersistence.createQuery(Pipeline.class).count();
    if (pipelineCount == 0) {
      return;
    }

    long initialCount = wingsPersistence.createQuery(Counter.class)
                            .field("key")
                            .endsWith(ActionType.CREATE_PIPELINE.toString())
                            .count();

    assertEquals(0, initialCount);
    initPipelineCounters.migrate();

    long finalCount = wingsPersistence.createQuery(Counter.class)
                          .field("key")
                          .endsWith(ActionType.CREATE_PIPELINE.toString())
                          .count();

    assertNotEquals("new entry(ies) should be created in `limitCounters` collection after migration", 0, finalCount);
  }
}
