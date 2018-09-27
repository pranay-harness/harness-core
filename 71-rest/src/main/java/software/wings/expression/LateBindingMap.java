package software.wings.expression;

import java.util.HashMap;

public class LateBindingMap extends HashMap<String, Object> {
  @Override
  public Object get(Object key) {
    Object object = super.get(key);
    if (object instanceof LateBindingValue) {
      object = ((LateBindingValue) object).bind((String) key);
      synchronized (this) {
        put((String) key, object);
      }
    }

    return object;
  }
}
