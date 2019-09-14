package io.harness.logging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

@Slf4j
public class AutoLogContext implements AutoCloseable {
  private String key;

  protected AutoLogContext(String key, String value) {
    final String original = MDC.get(key);
    if (original != null) {
      if (!original.equals(value)) {
        logger.error("Key: {} initialized in the same thread with a second value '{}'. The original is: '{}'", key,
            value, original);
      }
      // The context for this key is already set. Do not initialize the key so we keep the context until the first
      // initializer is hit.
      return;
    }
    this.key = key;
    MDC.put(key, value);
  }

  @Override
  public void close() throws Exception {
    if (key != null) {
      MDC.remove(key);
    }
  }
}
