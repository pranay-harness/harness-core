package software.wings.utils;

import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.ResponseMessage.ResponseTypeEnum.ERROR;
import static software.wings.beans.ResponseMessage.ResponseTypeEnum.WARN;

import com.mongodb.DuplicateKeyException;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.beans.UuidAware;
import software.wings.exception.WingsException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
   * Checks that value is null.
   *
   * @param name  name of parameter.
   * @param value input value of parameter.
   */
  public static void nullCheck(String name, Object value) {
    if (value != null) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", name);
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
      throw prepareWingsException(INVALID_ARGUMENT, "arg", "Duplicate " + field + " " + value);
    }
  }

  public static <V> V duplicateCheck(Callable<V> runnable, String field, String value) {
    try {
      return runnable.call();
    } catch (DuplicateKeyException e) {
      /*List<ResponseMessage> responseMessages = new ArrayList<>();
      responseMessages.add(prepareResponseMessage(INVALID_ARGUMENT, WARN, "hello"));
      Map<String, Object> params = new HashMap();
      params.put("args", "hello");
      throw new WingsException(responseMessages, "hello", params);*/
      //      throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", "Duplicate " + field + " " + value);
      throw prepareWingsException(INVALID_ARGUMENT, "args", "Duplicate " + field + " " + value);
    } catch (Exception e) {
      if (e.getCause() != null && e.getCause() instanceof DuplicateKeyException) {
        throw prepareWingsException(INVALID_ARGUMENT, "args", "Duplicate " + field + " " + value);
      }
      throw prepareWingsException(INVALID_ARGUMENT, "args", "Duplicate " + field + " " + value);
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
      ErrorCode errorCode, ResponseTypeEnum responseType, String param, String message) {
    List<ResponseMessage> responseMessages = new ArrayList<>();
    responseMessages.add(prepareResponseMessage(errorCode, responseType, message));
    Map<String, Object> params = new HashMap();
    params.put(param, message);
    return new WingsException(responseMessages, message, params);
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
   * @param errorType
   * @param errorMsg
   * @return
   */
  public static ResponseMessage prepareResponseMessage(
      ErrorCode errorCode, ResponseTypeEnum errorType, String errorMsg) {
    return ResponseMessage.builder()
        .code(errorCode)
        .errorType(errorType == null ? ERROR : errorType)
        .message(errorMsg)
        .build();
  }
}
