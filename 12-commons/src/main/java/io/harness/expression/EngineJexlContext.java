package io.harness.expression;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.JexlContext;

import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;

@Value
@Slf4j
public class EngineJexlContext implements JexlContext {
  EngineExpressionEvaluator engineExpressionEvaluator;
  Map<String, Object> originalMap;
  Map<String, Object> updatesMap;

  public EngineJexlContext(
      @NotNull EngineExpressionEvaluator engineExpressionEvaluator, @NotNull Map<String, Object> originalMap) {
    this.engineExpressionEvaluator = engineExpressionEvaluator;
    this.originalMap = originalMap;
    this.updatesMap = new HashMap<>();
  }

  @Override
  public synchronized boolean has(String key) {
    return originalMap.containsKey(key) || updatesMap.containsKey(key);
  }

  @Override
  public synchronized Object get(String key) {
    Object object = null;
    if (updatesMap.containsKey(key)) {
      object = updatesMap.get(key);
    }
    if (object == null && originalMap.containsKey(key)) {
      object = originalMap.get(key);
    }

    if (object instanceof LateBindingValue) {
      originalMap.remove(key);
      object = ((LateBindingValue) object).bind();
      originalMap.put(key, object);
    }
    return object;
  }

  @Override
  public synchronized void set(String name, Object value) {
    updatesMap.put(name, value);
  }

  public synchronized void clear() {
    originalMap.clear();
    updatesMap.clear();
  }
}
