package io.harness.logging;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class AutoLogContext implements AutoCloseable {
  private List<MDCCloseable> handles;

  public AutoLogContext(Map<String, String> values) {
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
    if (handles == null) {
      handles = new ArrayList<>();
    }
    handles.add(MDC.putCloseable(key, value));
  }

  @Override
  public void close() {
    if (isNotEmpty(handles)) {
      handles.forEach(handle -> handle.close());
    }
  }
}
