package software.wings.utils;

import com.google.api.client.util.Throwables;

import com.mongodb.DuplicateKeyException;
import software.wings.beans.ErrorCode;
import software.wings.beans.UuidAware;
import software.wings.exception.WingsException;

import java.util.Objects;
import java.util.concurrent.Callable;

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
   * @param value1 value1
   * @param value2  value2
   */
  public static void equalCheck(Object value1, Object value2) {
    if (!Objects.equals(value1, value2)) {
      throw new WingsException(
          ErrorCode.INVALID_REQUEST, "message", "Not equal -  value1: " + value1 + ", value2: " + value2);
    }
  }

  public static void validateUuid(UuidAware base, String fieldName, String fieldValue) {
    notNullCheck(fieldValue, fieldName);
    if (!fieldValue.equals(base.getUuid())) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", fieldName + " mismatch with object uuid");
    }
  }

  public static void duplicateCheck(Runnable runnable, String field, String value) {
    try {
      runnable.run();
    } catch (DuplicateKeyException e) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", "Duplicate " + field + " " + value);
    }
  }

  public static <V> V duplicateCheck(Callable<V> runnable, String field, String value) {
    try {
      return runnable.call();
    } catch (DuplicateKeyException e) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", "Duplicate " + field + " " + value);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
