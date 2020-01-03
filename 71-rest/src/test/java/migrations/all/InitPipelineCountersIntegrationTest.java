package migrations.all;

import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import io.harness.limits.ActionType;
import io.harness.limits.Counter;
import io.harness.rule.Owner;
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
  @Owner(developers = UJJAWAL)
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

    assertThat(initialCount).isEqualTo(0);
    initPipelineCounters.migrate();

    long finalCount = wingsPersistence.createQuery(Counter.class)
                          .field("key")
                          .endsWith(ActionType.CREATE_PIPELINE.toString())
                          .count();

    assertThat(0).isNotEqualTo(finalCount);
  }
}
