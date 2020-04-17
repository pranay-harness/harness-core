package io.harness.engine;

import static io.harness.exception.WingsException.ExecutionContext.MANAGER;

import io.harness.annotations.Redesign;
import io.harness.exception.WingsException;
import io.harness.logging.AutoLogContext;
import io.harness.logging.ExceptionLogger;
import io.harness.state.io.ambiance.Ambiance;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@Builder
@Slf4j
@Redesign
public class ExecutionEngineDispatcher implements Runnable {
  Ambiance ambiance;
  ExecutionEngine executionEngine;

  @Override
  public void run() {
    try (AutoLogContext ignore = ambiance.autoLogContext()) {
      executionEngine.startNodeExecution(ambiance);
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception exception) {
      logger.error("Unhandled exception", exception);
    }
  }
}