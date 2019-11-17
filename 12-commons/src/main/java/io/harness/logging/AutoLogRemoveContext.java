package io.harness.logging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

@Slf4j
public class AutoLogRemoveContext implements AutoCloseable {
  private String key;
  private String original;

  public AutoLogRemoveContext(String key) {
    original = MDC.get(key);
    if (original != null) {
      this.key = key;
      MDC.remove(key);
    }
  }

  @Override
  public void close() {
    if (original != null) {
      MDC.put(key, original);
    }
  }
}
