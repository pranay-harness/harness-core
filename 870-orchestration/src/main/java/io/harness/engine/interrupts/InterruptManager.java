package io.harness.engine.interrupts;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.interrupts.Interrupt;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.logging.AutoLogContext;

import com.google.inject.Inject;
import java.time.Duration;

@OwnedBy(PIPELINE)
public class InterruptManager {
  private static final String LOCK_NAME_PREFIX = "PLAN_EXECUTION_INFO_";
  @Inject private InterruptHandlerFactory interruptHandlerFactory;
  @Inject PersistentLocker persistentLocker;

  public Interrupt register(InterruptPackage interruptPackage) {
    Interrupt interrupt = Interrupt.builder()
                              .uuid(generateUuid())
                              .planExecutionId(interruptPackage.getPlanExecutionId())
                              .type(interruptPackage.getInterruptType())
                              .metadata(interruptPackage.getMetadata())
                              .nodeExecutionId(interruptPackage.getNodeExecutionId())
                              .parameters(interruptPackage.getParameters())
                              .interruptConfig(interruptPackage.getInterruptConfig())
                              .build();
    try (AcquiredLock<?> lock = persistentLocker.tryToAcquireLock(
             LOCK_NAME_PREFIX + interrupt.getPlanExecutionId(), Duration.ofSeconds(15));
         AutoLogContext ignore = interrupt.autoLogContext()) {
      if (lock == null) {
        throw new InvalidRequestException("Cannot register the interrupt. Please retry.");
      }
      InterruptHandler interruptHandler = interruptHandlerFactory.obtainHandler(interruptPackage.getInterruptType());
      return interruptHandler.registerInterrupt(interrupt);
    }
  }
}
