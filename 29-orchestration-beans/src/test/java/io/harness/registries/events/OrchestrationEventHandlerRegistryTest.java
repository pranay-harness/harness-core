package io.harness.registries.events;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationBeansTest;
import io.harness.category.element.UnitTests;
import io.harness.execution.events.AsyncOrchestrationEventHandlerProxy;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.OrchestrationEventHandler;
import io.harness.execution.events.OrchestrationEventHandlerProxy;
import io.harness.execution.events.OrchestrationEventType;
import io.harness.execution.events.OrchestrationSubject;
import io.harness.execution.events.SyncOrchestrationEventHandler;
import io.harness.execution.events.SyncOrchestrationEventHandlerProxy;
import io.harness.observer.Subject;
import io.harness.registries.RegistryType;
import io.harness.rule.Owner;
import org.joor.Reflect;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

public class OrchestrationEventHandlerRegistryTest extends OrchestrationBeansTest {
  @Inject OrchestrationEventHandlerRegistry registry;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRegisterAndObtain() {
    registry.register(OrchestrationEventType.ORCHESTRATION_START, StartHandler1.class);
    registry.register(OrchestrationEventType.ORCHESTRATION_START, StartHandler2.class);
    Subject<OrchestrationEventHandlerProxy> subject = registry.obtain(OrchestrationEventType.ORCHESTRATION_START);
    assertThat(subject).isNotNull();
    assertThat(subject).isInstanceOf(OrchestrationSubject.class);
    List<OrchestrationEventHandler> eventHandlers = Reflect.on(subject).get("observers");
    assertThat(eventHandlers).isNotEmpty();
    assertThat(eventHandlers).hasSize(2);
    assertThat(eventHandlers.get(0)).isInstanceOf(SyncOrchestrationEventHandlerProxy.class);
    assertThat(eventHandlers.get(1)).isInstanceOf(AsyncOrchestrationEventHandlerProxy.class);
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

  private static class StartHandler2 implements OrchestrationEventHandler {
    @Override
    public void handleEvent(OrchestrationEvent event) {}
  }
}