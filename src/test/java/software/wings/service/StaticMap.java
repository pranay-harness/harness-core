/**
 *
 */
package software.wings.service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Rishi
 *
 */
public class StaticMap {
  private static Map<String, Object> map = new HashMap<>();

  public static void putValue(String key, Object value) {
    map.put(key, value);
  }
  public static Object getValue(String key) {
    return map.get(key);
  }
}
