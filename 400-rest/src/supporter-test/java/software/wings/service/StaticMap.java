/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

/**
 *
 */

package software.wings.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The Class StaticMap.
 *
 * @author Rishi
 */
public class StaticMap {
  private static AtomicLong unique = new AtomicLong();

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

  public static long getUnique() {
    return unique.addAndGet(1);
  }
}
