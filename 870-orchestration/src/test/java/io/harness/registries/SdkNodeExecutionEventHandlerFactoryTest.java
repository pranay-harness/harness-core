package io.harness.registries;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.event.handlers.AddExecutableResponseEventHandler;
import io.harness.event.handlers.HandleStepResponseEventHandler;
import io.harness.event.handlers.QueueNodeExecutionEventHandler;
import io.harness.pms.contracts.execution.events.SdkResponseEventType;
import io.harness.rule.Owner;

import com.google.inject.Injector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class SdkNodeExecutionEventHandlerFactoryTest extends OrchestrationTestBase {
  @Mock Injector injector;

  @InjectMocks SdkNodeExecutionEventHandlerFactory sdkNodeExecutionEventHandlerFactory;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void verifyMocks() {
    verifyNoMoreInteractions(injector);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetHandler() {
    Mockito.when(injector.getInstance(QueueNodeExecutionEventHandler.class)).thenReturn(null);
    Mockito.when(injector.getInstance(AddExecutableResponseEventHandler.class)).thenReturn(null);

    sdkNodeExecutionEventHandlerFactory.getHandler(SdkResponseEventType.QUEUE_NODE);
    verify(injector).getInstance(QueueNodeExecutionEventHandler.class);

    sdkNodeExecutionEventHandlerFactory.getHandler(SdkResponseEventType.ADD_EXECUTABLE_RESPONSE);
    verify(injector).getInstance(AddExecutableResponseEventHandler.class);

    sdkNodeExecutionEventHandlerFactory.getHandler(SdkResponseEventType.HANDLE_STEP_RESPONSE);
    verify(injector).getInstance(HandleStepResponseEventHandler.class);
  }
}