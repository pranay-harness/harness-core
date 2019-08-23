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
import software.wings.beans.InfrastructureProvisioner;
import software.wings.integration.BaseIntegrationTest;

public class InitInfraProvisionerCountersIntegrationTest extends BaseIntegrationTest {
  @Inject private InitInfraProvisionerCounters initInfraProvisionerCounters;

  @Before
  public void init() {
    wingsPersistence.delete(wingsPersistence.createQuery(Counter.class)
                                .field("key")
                                .endsWith(ActionType.CREATE_INFRA_PROVISIONER.toString()));
  }

  @Test
  @Category(IntegrationTests.class)
  public void testMigrate() {
    long infraProvisionerCount = wingsPersistence.createQuery(InfrastructureProvisioner.class).count();
    if (infraProvisionerCount == 0) {
      return;
    }

    long initialCount = wingsPersistence.createQuery(Counter.class)
                            .field("key")
                            .endsWith(ActionType.CREATE_INFRA_PROVISIONER.toString())
                            .count();

    assertThat(initialCount).isEqualTo(0);
    initInfraProvisionerCounters.migrate();

    long finalCount = wingsPersistence.createQuery(Counter.class)
                          .field("key")
                          .endsWith(ActionType.CREATE_INFRA_PROVISIONER.toString())
                          .count();

    assertNotEquals("new entry(ies) should be created in `limitCounters` collection after migration", 0, finalCount);
  }
}
