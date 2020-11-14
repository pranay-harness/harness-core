package io.harness.registries.events;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationBeansTestBase;
import io.harness.category.element.UnitTests;
import io.harness.execution.events.AsyncOrchestrationEventHandler;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.OrchestrationEventHandler;
import io.harness.execution.events.OrchestrationEventType;
import io.harness.execution.events.SyncOrchestrationEventHandler;
import io.harness.registries.RegistryType;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Collections;
import java.util.Set;

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