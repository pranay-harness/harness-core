package io.harness.expression;

import java.util.HashMap;

public class LateBindingMap extends HashMap<String, Object> {
  @Override
  public synchronized Object get(Object key) {
    Object object = super.get(key);
    if (object instanceof LateBindingValue) {
      // Remove the late binding value to avoid endless loop
      remove(key);
      object = ((LateBindingValue) object).bind();
      put((String) key, object);
    }

    return object;
  }
}
