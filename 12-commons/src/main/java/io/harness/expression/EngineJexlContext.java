package io.harness.expression;

import static java.lang.String.format;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.FunctorException;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

@Value
@Slf4j
public class EngineJexlContext implements JexlContext {
  EngineExpressionEvaluator engineExpressionEvaluator;
  List<String> prefixes;
  Map<String, Object> originalMap;
  Map<String, Object> updatesMap;
  @NonFinal boolean recursive;

  public EngineJexlContext(@NotNull EngineExpressionEvaluator engineExpressionEvaluator,
      @NotNull Map<String, Object> originalMap, @NotNull List<String> prefixes) {
    this.engineExpressionEvaluator = engineExpressionEvaluator;
    this.prefixes = prefixes;
    this.originalMap = originalMap;
    this.updatesMap = new HashMap<>();
    this.recursive = false;
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
    } else if (recursive) {
      object = originalMap.get(key);
    } else {
      for (String prefix : prefixes) {
        String prefixedKey = EmptyPredicate.isEmpty(prefix) ? key : prefix + "." + key;
        object = originalMap.get(prefixedKey);
        if (object == null) {
          recursive = true;
          try {
            object = engineExpressionEvaluator.evaluate(prefixedKey, this);
          } catch (JexlException ex) {
            if (ex.getCause() instanceof FunctorException) {
              throw(FunctorException) ex.getCause();
            }

            logger.debug(format("Failed to evaluate prefixed key: %s", prefixedKey), ex);
          } finally {
            recursive = false;
          }
        }

        if (object != null) {
          break;
        }
      }
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
