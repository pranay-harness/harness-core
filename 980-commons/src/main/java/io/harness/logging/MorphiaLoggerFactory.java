package io.harness.logging;

public class MorphiaLoggerFactory implements org.mongodb.morphia.logging.LoggerFactory {
  @Override
  public org.mongodb.morphia.logging.Logger get(Class<?> c) {
    return new MorphiaLogger(c);
  }
}
