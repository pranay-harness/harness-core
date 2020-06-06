package io.harness.engine.interrupts.handlers;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.execution.status.Status.retryableStatuses;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.engine.helpers.RetryHelper;
import io.harness.engine.interrupts.InterruptHandler;
import io.harness.engine.services.NodeExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.Interrupt.InterruptKeys;
import io.harness.interrupts.Interrupt.State;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

public class RetryInterruptHandler implements InterruptHandler {
  @Inject private RetryHelper retryHelper;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject @Named("enginePersistence") private HPersistence hPersistence;

  @Override
  public Interrupt registerInterrupt(Interrupt interrupt) {
    String savedInterruptId = validateAndSave(interrupt);
    Interrupt savedInterrupt = buildQuery(savedInterruptId).get();
    return handleInterrupt(savedInterrupt);
  }

  private Query<Interrupt> buildQuery(String savedInterruptId) {
    return hPersistence.createQuery(Interrupt.class).filter(InterruptKeys.uuid, savedInterruptId);
  }

  private String validateAndSave(Interrupt interrupt) {
    if (isEmpty(interrupt.getNodeExecutionId())) {
      throw new InvalidRequestException("NodeExecutionId Cannot be empty for RETRY interrupt");
    }
    NodeExecution nodeExecution = nodeExecutionService.get(interrupt.getNodeExecutionId());
    if (!retryableStatuses().contains(nodeExecution.getStatus())) {
      throw new InvalidRequestException(
          "NodeExecution is not in a retryable status. Current Status: " + nodeExecution.getStatus());
    }
    interrupt.setState(State.PROCESSING);
    return hPersistence.save(interrupt);
  }

  @Override
  public Interrupt handleInterrupt(Interrupt interrupt) {
    retryHelper.retryNodeExecution(interrupt.getNodeExecutionId());
    UpdateOperations<Interrupt> ops =
        hPersistence.createUpdateOperations(Interrupt.class).set(InterruptKeys.state, State.PROCESSED_SUCCESSFULLY);
    return hPersistence.findAndModify(buildQuery(interrupt.getUuid()), ops, HPersistence.returnNewOptions);
  }

  @Override
  public Interrupt handleInterruptForNodeExecution(Interrupt interrupt, String nodeExecutionId) {
    throw new UnsupportedOperationException("Please use handleInterrupt for handling retries");
  }
}
