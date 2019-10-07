package io.harness.expression;

import lombok.Builder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlException;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

@Builder
public class LateBindingContext implements JexlContext {
  private ExpressionEvaluator expressionEvaluator;
  private List<String> prefixes;
  private ConcurrentMap<String, Object> map;

  private boolean recursive;

  @Override
  public boolean has(String name) {
    return map.containsKey(name);
  }

  @Override
  public Object get(String key) {
    Object object = map.get(key);
    if (object == null && !recursive) {
      for (String prefix : prefixes) {
        if (prefix == null) {
          continue;
        }
        recursive = true;
        try {
          object = expressionEvaluator.evaluate(prefix + "." + key, this);
        } catch (JexlException ignore) {
          // Ignore any expression evaluation exception
        } finally {
          recursive = false;
        }
        if (object != null) {
          break;
        }
      }
    }

    if (object instanceof LateBindingValue) {
      map.remove((String) key);
      object = ((LateBindingValue) object).bind();
      map.put((String) key, object);
    }

    return object;
  }

  @Override
  public void set(String name, Object value) {
    map.put(name, value);
  }

  public void clear() {
    map.clear();
  }
}
