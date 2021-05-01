package io.harness.registries;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.event.handlers.AddExecutableResponseEventHandler;
import io.harness.event.handlers.AdviserEventResponseHandler;
import io.harness.event.handlers.ErrorEventResponseHandler;
import io.harness.event.handlers.FacilitateResponseRequestHandler;
import io.harness.event.handlers.HandleStepResponseEventHandler;
import io.harness.event.handlers.QueueNodeExecutionEventHandler;
import io.harness.event.handlers.QueueTaskAndAddExecutableResponseHandler;
import io.harness.event.handlers.ResumeNodeExecutionResponseEventHandler;
import io.harness.event.handlers.SdkResponseEventHandler;
import io.harness.event.handlers.SpawnChildResponseEventHandler;
import io.harness.event.handlers.SuspendChainResponseEventHandler;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.execution.events.SdkResponseEventType;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class SdkNodeExecutionEventHandlerFactory {
  @Inject Injector injector;

  public SdkResponseEventHandler getHandler(SdkResponseEventType eventType) {
    switch (eventType) {
      case QUEUE_NODE:
        return injector.getInstance(QueueNodeExecutionEventHandler.class);
      case ADD_EXECUTABLE_RESPONSE:
        return injector.getInstance(AddExecutableResponseEventHandler.class);
      case HANDLE_STEP_RESPONSE:
        return injector.getInstance(HandleStepResponseEventHandler.class);
      case RESUME_NODE_EXECUTION:
        return injector.getInstance(ResumeNodeExecutionResponseEventHandler.class);
      case HANDLE_FACILITATE_RESPONSE:
        return injector.getInstance(FacilitateResponseRequestHandler.class);
      case HANDLE_EVENT_ERROR:
        return injector.getInstance(ErrorEventResponseHandler.class);
      case HANDLE_ADVISER_RESPONSE:
        return injector.getInstance(AdviserEventResponseHandler.class);
      case QUEUE_TASK_AND_ADD_EXECUTABLE_RESPONSE:
        return injector.getInstance(QueueTaskAndAddExecutableResponseHandler.class);
      case SUSPEND_CHAIN:
        return injector.getInstance(SuspendChainResponseEventHandler.class);
      case SPAWN_CHILD:
        return injector.getInstance(SpawnChildResponseEventHandler.class);
      default:
        throw new InvalidRequestException("Unknown sdkResponseEventType.");
    }
  }
}
