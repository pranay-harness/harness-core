package software.wings.utils;

import static software.wings.beans.ErrorCode.GENERAL_ERROR;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.ResponseMessage.Level.ERROR;
import static software.wings.beans.ResponseMessage.Level.WARN;
import static software.wings.beans.ResponseMessage.aResponseMessage;

import com.mongodb.DuplicateKeyException;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;
import software.wings.beans.ResponseMessage.Level;
import software.wings.beans.UuidAware;
import software.wings.exception.WingsException;
import software.wings.exception.WingsException.ReportTarget;

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
      throw new WingsException(GENERAL_ERROR).addParam("args", name);
    }
  }

  /**
   * Checks that value is not null.
   *
   * @param name  name of parameter.
   * @param value input value of parameter.
   */
  public static void notNullCheck(String name, Object value, ReportTarget[] reportTargets) {
    if (value == null) {
      throw new WingsException(GENERAL_ERROR, reportTargets).addParam("args", name);
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
      throw new WingsException(GENERAL_ERROR).addParam("args", name);
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
      throw new WingsException(INVALID_REQUEST)
          .addParam("message", "Not equal -  value1: " + value1 + ", value2: " + value2);
    }
  }

  public static void unEqualCheck(Object value1, Object value2) {
    if (Objects.equals(value1, value2)) {
      throw new WingsException(INVALID_REQUEST)
          .addParam("message", "Equal -  value1: " + value1 + ", value2: " + value2);
    }
  }

  public static void validateUuid(UuidAware base, String fieldName, String fieldValue) {
    notNullCheck(fieldValue, fieldName);
    if (!fieldValue.equals(base.getUuid())) {
      throw new WingsException(INVALID_REQUEST).addParam("message", fieldName + " mismatch with object uuid");
    }
  }

  public static void duplicateCheck(Runnable runnable, String field, String value) {
    try {
      runnable.run();
    } catch (DuplicateKeyException e) {
      throw prepareWingsException(GENERAL_ERROR, "args", "Duplicate " + field + " " + value);
    }
  }

  public static <V> V duplicateCheck(Callable<V> runnable, String field, String value) {
    try {
      return runnable.call();
    } catch (DuplicateKeyException e) {
      throw prepareWingsException(GENERAL_ERROR, "args", "Duplicate " + field + " " + value);
    } catch (Exception e) {
      if (e.getCause() != null && e.getCause() instanceof DuplicateKeyException) {
        throw prepareWingsException(GENERAL_ERROR, "args", "Duplicate " + field + " " + value);
      }
      throw prepareWingsException(GENERAL_ERROR, "args", e.getMessage());
    }
  }

  /***
   * Prepares WingsException
   * @param errorCode
   * @param responseType
   * @param param
   * @param message
   * @return
   */
  public static WingsException prepareWingsException(
      ErrorCode errorCode, Level responseType, String param, String message) {
    return new WingsException(prepareResponseMessage(errorCode, responseType, message)).addParam(param, message);
  }
  /**
   * Prepares and throw exception with WARN error type.
   *
   * @param errorCode
   * @param param
   * @param message
   */
  public static WingsException prepareWingsException(ErrorCode errorCode, String param, String message) {
    return prepareWingsException(errorCode, WARN, param, message);
  }

  /**
   * Prepares Response Message
   * @param errorCode
   * @param level
   * @param errorMsg
   * @return
   */
  public static ResponseMessage prepareResponseMessage(ErrorCode errorCode, Level level, String errorMsg) {
    return aResponseMessage().code(errorCode).level(level == null ? ERROR : level).message(errorMsg).build();
  }
}
