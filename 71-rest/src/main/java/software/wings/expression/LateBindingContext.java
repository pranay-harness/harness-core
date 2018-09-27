package software.wings.expression;

import org.apache.commons.jexl3.JexlContext;

import java.util.HashMap;
import java.util.Map;

public class LateBindingContext implements JexlContext {
  private final Map<String, Object> map = new HashMap();

  public LateBindingContext() {}

  public LateBindingContext(Map<String, Object> vars) {
    map.putAll(vars);
  }

  public boolean has(String name) {
    return map.containsKey(name);
  }

  public Object get(String key) {
    Object object = map.get(key);
    if (object instanceof LateBindingValue) {
      object = ((LateBindingValue) object).bind(key);
      synchronized (map) {
        map.put((String) key, object);
      }
    }

    return object;
  }

  public void set(String name, Object value) {
    map.put(name, value);
  }

  public void clear() {
    map.clear();
  }
}
