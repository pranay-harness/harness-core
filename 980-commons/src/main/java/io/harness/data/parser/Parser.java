/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.data.parser;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Parser {
  public static int asInt(String value) {
    return asInt(value, 0);
  }

  public static int asInt(String value, int defaultValue) {
    try {
      return Integer.parseInt(value);
    } catch (Exception exception) {
      return defaultValue;
    }
  }
}
