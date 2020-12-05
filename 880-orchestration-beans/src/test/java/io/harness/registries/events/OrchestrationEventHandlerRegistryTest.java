package io.harness.registries.events;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationBeansTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.pms.sdk.core.events.SyncOrchestrationEventHandler;
import io.harness.registries.RegistryType;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class OrchestrationEventHandlerRegistryTest extends OrchestrationBeansTestBase {
  @Inject OrchestrationEventHandlerRegistry registry;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRegisterAndObtain() {
    registry.register(OrchestrationEventType.ORCHESTRATION_START, Collections.singleton(new StartHandler1()));
    registry.register(OrchestrationEventType.ORCHESTRATION_START, Collections.singleton(new StartHandler2()));
    Set<OrchestrationEventHandler> handlers = registry.obtain(OrchestrationEventType.ORCHESTRATION_START);
    assertThat(handlers).isNotEmpty();
    assertThat(handlers).hasSize(2);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void getType() {
    assertThat(registry.getType()).isEqualTo(RegistryType.ORCHESTRATION_EVENT.name());
  }

  private static class StartHandler1 implements SyncOrchestrationEventHandler {
    @Override
    public void handleEvent(OrchestrationEvent event) {}
  }

  private static class StartHandler2 implements AsyncOrchestrationEventHandler {
    @Override
    public void handleEvent(OrchestrationEvent event) {}
  }
}
