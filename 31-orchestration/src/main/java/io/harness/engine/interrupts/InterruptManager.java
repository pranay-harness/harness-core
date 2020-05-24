package io.harness.engine.interrupts;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;

import io.harness.beans.EmbeddedUser;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.interrupts.Interrupt;
import lombok.NonNull;

public class InterruptManager {
  @Inject private InterruptHandlerFactory interruptHandlerFactory;

  public Interrupt register(@NonNull String planExecutionId, @NonNull ExecutionInterruptType interruptType,
      @NonNull EmbeddedUser user, String nodeExecutionId) {
    Interrupt interrupt = Interrupt.builder()
                              .uuid(generateUuid())
                              .planExecutionId(planExecutionId)
                              .type(interruptType)
                              .createdBy(user)
                              .nodeExecutionId(nodeExecutionId)
                              .build();
    InterruptHandler interruptHandler = interruptHandlerFactory.obtainHandler(interruptType);
    return interruptHandler.handleInterrupt(interrupt);
  }
}
