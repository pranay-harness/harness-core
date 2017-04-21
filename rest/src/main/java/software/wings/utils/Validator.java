package software.wings.utils;

import software.wings.beans.ErrorCode;
import software.wings.beans.UuidAware;
import software.wings.exception.WingsException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The Class Validator.
 */
public class Validator {
  /**
   * Checks that value is not null.
   *
   * @param name  name of parameter.
   * @param value input value of parameter.
   */
  public static void notNullCheck(String name, Object value) {
    if (value == null) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", name);
    }
  }

  /**
   * Checks whether 2 values are equal or not.
   *
   * @param appId value1
   * @param uuid  value2
   */
  public static void equalCheck(Object appId, Object uuid) {
    if (!Objects.equals(appId, uuid)) {
      Map<String, Object> map = new HashMap<>();
      map.put("appId", appId);
      map.put("uuid", uuid);
      throw new WingsException(map, ErrorCode.INVALID_ARGUMENT);
    }
  }

  public static void validateUuid(UuidAware base, String fieldName, String fieldValue) {
    notNullCheck(fieldValue, fieldName);
    if (!fieldValue.equals(base.getUuid())) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", fieldName + " mismatch with object uuid");
    }
  }
}
