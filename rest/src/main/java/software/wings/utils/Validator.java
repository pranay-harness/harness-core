package software.wings.utils;

import static software.wings.beans.ErrorCode.GENERAL_ERROR;
import static software.wings.exception.WingsException.USER;

import com.mongodb.DuplicateKeyException;
import io.harness.persistence.UuidAware;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.exception.WingsException.ReportTarget;

import java.util.EnumSet;
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
      throw new WingsException(GENERAL_ERROR).addParam("message", name);
    }
  }

  /**
   * Checks that value is not null.
   *
   * @param name  name of parameter.
   * @param value input value of parameter.
   */
  public static void notNullCheck(String name, Object value, EnumSet<ReportTarget> reportTargets) {
    if (value == null) {
      throw new WingsException(GENERAL_ERROR, reportTargets).addParam("message", name);
    }
  }

  /**
   * Checks that value is null.
   *
   * @param name  name of parameter.
   * @param value input value of parameter.
   */
  public static void nullCheck(String name, Object value) {
    if (value != null) {
      throw new WingsException(GENERAL_ERROR).addParam("message", name);
    }
  }

  /**
   * Checks whether 2 values are equal or not.
   *
   * @param value1 value1
   * @param value2 value2
   */
  public static void equalCheck(Object value1, Object value2) {
    if (!Objects.equals(value1, value2)) {
      throw new InvalidRequestException("Not equal -  value1: " + value1 + ", value2: " + value2);
    }
  }

  public static void unEqualCheck(Object value1, Object value2) {
    if (Objects.equals(value1, value2)) {
      throw new InvalidRequestException("Equal -  value1: " + value1 + ", value2: " + value2);
    }
  }

  public static void validateUuid(UuidAware base, String fieldName, String fieldValue) {
    notNullCheck(fieldValue, fieldName);
    if (!fieldValue.equals(base.getUuid())) {
      throw new InvalidRequestException(fieldName + " mismatch with object uuid");
    }
  }

  public static void duplicateCheck(Runnable runnable, String field, String value) {
    try {
      runnable.run();
    } catch (DuplicateKeyException e) {
      new WingsException(GENERAL_ERROR).addParam("message", "Duplicate " + field + " " + value);
    }
  }

  public static <V> V duplicateCheck(Callable<V> runnable, String field, String value) {
    try {
      return runnable.call();
    } catch (DuplicateKeyException e) {
      throw new WingsException(GENERAL_ERROR, USER).addParam("message", "Duplicate " + field + " " + value);
    } catch (Exception e) {
      if (e.getCause() != null && e.getCause() instanceof DuplicateKeyException) {
        throw new WingsException(GENERAL_ERROR, USER).addParam("message", "Duplicate " + field + " " + value);
      }
      throw new WingsException(GENERAL_ERROR, USER).addParam("message", Misc.getMessage(e));
    }
  }
}
