package io.harness.engine.events;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandlerProxy;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationSubject;
import io.harness.pms.sdk.core.events.SyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.SyncOrchestrationEventHandlerProxy;
import io.harness.pms.sdk.core.execution.SdkOrchestrationEventListener;
import io.harness.pms.sdk.core.registries.OrchestrationEventHandlerRegistry;
import io.harness.rule.Owner;
import io.harness.utils.AmbianceTestUtils;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PIPELINE)
public class OrchestrationEventEmitterTest extends OrchestrationTestBase {
  @InjectMocks @Inject private OrchestrationEventEmitter eventEmitter;
  @InjectMocks @Inject private SdkOrchestrationEventListener eventListener;
  @Mock OrchestrationEventHandlerRegistry registry;

  @Inject private Injector injector;

  @Test
  @Owner(developers = PRASHANT, intermittent = true)
  @Category(UnitTests.class)
  public void shouldTestEmitEvent() {
    OrchestrationSubject subject = spy(new OrchestrationSubject());
    SyncOrchestrationEventHandlerProxy syncProxy =
        spy(SyncOrchestrationEventHandlerProxy.builder().eventHandler(new StartHandler1()).build());
    AsyncOrchestrationEventHandlerProxy asyncProxy =
        spy(AsyncOrchestrationEventHandlerProxy.builder().eventHandler(new StartHandler2()).build());
    subject.registerSyncHandler(syncProxy);
    subject.registerAsyncHandler(asyncProxy);

    when(registry.obtain(OrchestrationEventType.ORCHESTRATION_START)).thenReturn(ImmutableSet.of());
    OrchestrationEvent event = OrchestrationEvent.builder()
                                   .ambiance(AmbianceTestUtils.buildAmbiance())
                                   .eventType(OrchestrationEventType.ORCHESTRATION_START)
                                   .build();
    eventEmitter.emitEvent(event);
    eventListener.onMessage(event);
    verify(subject, times(1)).handleEventSync(eq(event));
    verify(subject, times(1)).handleEventAsync(eq(event));
    verify(syncProxy, times(1)).handleEvent(eq(event));
    verify(asyncProxy, times(1)).handleEvent(eq(event));
    verify(asyncProxy, times(1)).getInformExecutorService();
    verifyNoMoreInteractions(asyncProxy);
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
