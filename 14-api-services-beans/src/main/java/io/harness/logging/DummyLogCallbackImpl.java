package io.harness.logging;

import lombok.extern.log4j.Log4j;

@Log4j
public class DummyLogCallbackImpl implements LogCallback {
  @Override
  public void saveExecutionLog(String line) {
    logger.info(line);
  }

  @Override
  public void saveExecutionLog(String line, LogLevel logLevel) {
    logger.info(line);
  }

  @Override
  public void saveExecutionLog(String line, LogLevel logLevel, CommandExecutionStatus commandExecutionStatus) {
    logger.info(line);
  }
}
