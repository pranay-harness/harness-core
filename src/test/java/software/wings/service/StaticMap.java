/**
 *
 */

package software.wings.service;

import java.util.HashMap;
import java.util.Map;

// TODO: Auto-generated Javadoc

/**
 * The Class StaticMap.
 *
 * @author Rishi
 */
public class StaticMap {
  private static Map<String, Object> map = new HashMap<>();

  /**
   * Put value.
   *
   * @param key   the key
   * @param value the value
   */
  public static void putValue(String key, Object value) {
    map.put(key, value);
  }

  /**
   * Gets the value.
   *
   * @param key the key
   * @return the value
   */
  public static Object getValue(String key) {
    return map.get(key);
  }
}
