package software.wings.expression;

import java.util.HashMap;

public class LateBindingMap extends HashMap<String, Object> {
  @Override
  public Object get(Object key) {
    Object object = super.get(key);
    if (object instanceof LateBindingValue) {
      // Remove the late binding value to avoid endless loop
      synchronized (this) {
        remove((String) key);
      }
      object = ((LateBindingValue) object).bind();
      synchronized (this) {
        put((String) key, object);
      }
    }

    return object;
  }
}
