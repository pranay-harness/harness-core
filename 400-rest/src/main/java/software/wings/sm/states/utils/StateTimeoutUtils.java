/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
