package io.harness.engine.events;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.category.element.UnitTests;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.execution.events.OrchestrationEventType;
import io.harness.execution.events.OrchestrationSubject;
import io.harness.registries.events.OrchestrationEventHandlerRegistry;
import io.harness.rule.Owner;
import io.harness.utils.AmbianceTestUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class OrchestrationEventEmitterTest extends OrchestrationTest {
  @InjectMocks @Inject private OrchestrationEventEmitter eventEmitter;
  @Mock OrchestrationEventHandlerRegistry registry;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestEmitEvent() {
    OrchestrationSubject subject = spy(new OrchestrationSubject());
    when(registry.obtain(OrchestrationEventType.ORCHESTRATION_START)).thenReturn(subject);
    OrchestrationEvent event = OrchestrationEvent.builder()
                                   .ambiance(AmbianceTestUtils.buildAmbiance())
                                   .eventType(OrchestrationEventType.ORCHESTRATION_START)
                                   .build();
    eventEmitter.emitEvent(event);
    verify(subject).fireInform(any(), eq(event));
  }
}