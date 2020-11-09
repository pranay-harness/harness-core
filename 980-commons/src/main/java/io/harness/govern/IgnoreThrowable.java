package io.harness.govern;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class IgnoreThrowable {
  public static void ignoredOnPurpose(Throwable exception) {
    // We would like to express explicitly that we are ignoring this exception
  }
}
