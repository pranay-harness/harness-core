package io.harness.engine;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.WingsException;
import io.harness.logging.AutoLogContext;
import io.harness.logging.ExceptionLogger;
import io.harness.state.io.StepTransput;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
@Slf4j
@Redesign
public class ExecutionEngineDispatcher implements Runnable {
  Ambiance ambiance;
  ExecutionEngine executionEngine;
  List<StepTransput> additionalInputs;

  @Override
  public void run() {
    try (AutoLogContext ignore = ambiance.autoLogContext()) {
      executionEngine.startNodeExecution(ambiance, additionalInputs);
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception exception) {
      logger.error("Unhandled exception", exception);
    }
  }
}