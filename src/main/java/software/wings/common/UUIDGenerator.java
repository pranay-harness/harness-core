package software.wings.common;

import java.util.UUID;
/**
 *  A common universal unique ID generator that will be used throughout the wings application.
 *
 *
 * @author Rishi
 *
 */
public class UUIDGenerator {
  public static String getUUID() {
    return UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
  }
}
