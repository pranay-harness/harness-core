package io.harness.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MorphiaLogger implements org.mongodb.morphia.logging.Logger {
  private Logger logger;

  MorphiaLogger(Class<?> c) {
    logger = LoggerFactory.getLogger(c);
  }

  @Override
  public void debug(String msg) {
    logger.debug(msg);
  }

  @Override
  public void debug(String msg, Object... arg) {
    logger.debug(msg, arg);
  }

  @Override
  public void debug(String msg, Throwable t) {
    logger.debug(msg, t);
  }

  @Override
  public void error(String msg) {
    logger.error(msg);
  }

  @Override
  public void error(String msg, Object... arg) {
    logger.error(msg, arg);
  }

  @Override
  public void error(String msg, Throwable t) {
    logger.error(msg, t);
  }

  @Override
  public void info(String msg) {
    logger.info(msg);
  }

  @Override
  public void info(String msg, Object... arg) {
    logger.info(msg, arg);
  }

  @Override
  public void info(String msg, Throwable t) {
    logger.info(msg, t);
  }

  @Override
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  @Override
  public boolean isErrorEnabled() {
    return logger.isErrorEnabled();
  }

  @Override
  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  @Override
  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  @Override
  public boolean isWarningEnabled() {
    return logger.isWarnEnabled();
  }

  @Override
  public void trace(String msg) {
    logger.trace(msg);
  }

  @Override
  public void trace(String msg, Object... arg) {
    logger.trace(msg, arg);
  }

  @Override
  public void trace(String msg, Throwable t) {
    logger.trace(msg, t);
  }

  @Override
  public void warning(String msg) {
    // suppress warning for elemMatch for InfrastructureProvisioner.mappingBlueprints
    if (("The type(s) for the query/update may be inconsistent; using an instance of type "
            + "'software.wings.dl.HQuery' for the field "
            + "'software.wings.beans.InfrastructureProvisioner.mappingBlueprints' "
            + "which is declared as 'java.util.List'")
            .equals(msg)) {
      return;
    }
    logger.warn(msg);
  }

  @Override
  public void warning(String msg, Object... arg) {
    logger.warn(msg, arg);
  }

  @Override
  public void warning(String msg, Throwable t) {
    logger.warn(msg, t);
  }
}