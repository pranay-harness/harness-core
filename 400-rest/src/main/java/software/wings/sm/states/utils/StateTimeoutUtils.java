/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states.utils;

import static java.time.Duration.ofMinutes;

import com.google.common.primitives.Ints;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StateTimeoutUtils {
  private StateTimeoutUtils() {}

  public static Integer getTimeoutMillisFromMinutes(Integer timeoutMinutes) {
    if (timeoutMinutes == null || timeoutMinutes == 0) {
      return null;
    }
    try {
      return Ints.checkedCast(ofMinutes(timeoutMinutes).toMillis());
    } catch (Exception e) {
      log.warn("Could not convert {} minutes to millis, falling back to default timeout", timeoutMinutes);
      return null;
    }
  }
}
