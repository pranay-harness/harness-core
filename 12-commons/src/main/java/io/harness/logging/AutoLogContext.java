package io.harness.logging;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
public class AutoLogContext implements AutoCloseable {
  private Set<String> keys = new HashSet<>();

  protected AutoLogContext(Map<String, String> values) {
    for (Map.Entry<String, String> entry : values.entrySet()) {
      addKeyValue(entry.getKey(), entry.getValue());
    }
  }
  protected AutoLogContext(String key, String value) {
    addKeyValue(key, value);
  }

  private void addKeyValue(String key, String value) {
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
    keys.add(key);
    MDC.put(key, value);
  }

  @Override
  public void close() {
    if (isNotEmpty(keys)) {
      for (String key : keys) {
        MDC.remove(key);
      }
    }
  }
}
