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
  @Owner(developers = ANKIT)
  @Category(DeprecatedIntegrationTests.class)
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

    assertThat(0).isNotEqualTo(finalCount);
  }
}
