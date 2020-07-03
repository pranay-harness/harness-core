package io.harness.registries.events;

import static io.harness.registries.RegistryType.ORCHESTRATION_EVENT;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationBeansTest;
import io.harness.category.element.UnitTests;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.OrchestrationEventHandler;
import io.harness.execution.events.OrchestrationEventType;
import io.harness.execution.events.OrchestrationSubject;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class OrchestrationEventHandlerRegistryTest extends OrchestrationBeansTest {
  @Inject OrchestrationEventHandlerRegistry registry;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRegisterAndObtain() {
    registry.register(OrchestrationEventType.ORCHESTRATION_START, StartHandler1.class);
    registry.register(OrchestrationEventType.ORCHESTRATION_START, StartHandler2.class);
    assertThat(registry.obtain(OrchestrationEventType.ORCHESTRATION_START)).isNotNull();
    assertThat(registry.obtain(OrchestrationEventType.ORCHESTRATION_START)).isInstanceOf(OrchestrationSubject.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void getType() {
    assertThat(registry.getType()).isEqualTo(ORCHESTRATION_EVENT);
  }

  private static class StartHandler1 implements OrchestrationEventHandler {
    @Override
    public void handleEvent(OrchestrationEvent event) {}
  }

  private static class StartHandler2 implements OrchestrationEventHandler {
    @Override
    public void handleEvent(OrchestrationEvent event) {}
  }
}