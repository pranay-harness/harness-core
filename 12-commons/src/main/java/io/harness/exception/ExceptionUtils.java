package io.harness.exception;

import static io.harness.exception.WingsException.ReportTarget.REST_API;
import static java.util.stream.Collectors.joining;

import io.harness.eraro.ResponseMessage;
import io.harness.logging.ExceptionLogger;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumSet;
import javax.validation.ConstraintViolationException;

@UtilityClass
@Slf4j
public class ExceptionUtils {
  public static <T> T cause(Class<T> clazz, Throwable exception) {
    while (exception != null) {
      if (exception.getClass().equals(clazz)) {
        return (T) exception;
      }
      exception = exception.getCause();
    }
    return null;
  }

  public static EnumSet<FailureType> getFailureTypes(Throwable throwable) {
    EnumSet<FailureType> failureTypes = EnumSet.noneOf(FailureType.class);

    Throwable t = throwable;
    while (t != null) {
      if (t instanceof WingsException) {
        failureTypes.addAll(((WingsException) t).getFailureTypes());
      }

      t = t.getCause();
    }

    if (failureTypes.isEmpty()) {
      logger.error("While determining the failureTypes, none was discovered for the following exception", throwable);
    }

    return failureTypes;
  }

  public static String getMessage(Throwable t) {
    if (t instanceof WingsException) {
      WingsException we = (WingsException) t;
      return ExceptionLogger.getResponseMessageList(we, REST_API)
          .stream()
          .map(ResponseMessage::getMessage)
          .collect(joining(". "));
    } else if (t instanceof ConstraintViolationException) {
      ConstraintViolationException constraintViolationException = (ConstraintViolationException) t;
      return constraintViolationException.getConstraintViolations()
          .stream()
          .map(item
              -> String.format("Constraint Violation. Class: %s, Field: %s, InvalidValue: %s, Message: %s",
                  item.getRootBeanClass(), item.getPropertyPath(), item.getInvalidValue(), item.getMessage()))
          .collect(joining(". "));
    } else if (t instanceof HarnessException) {
      HarnessException he = (HarnessException) t;
      Throwable cause = he.getCause();
      if (cause instanceof WingsException) {
        WingsException we = (WingsException) cause;
        return ExceptionLogger.getResponseMessageList(we, REST_API)
            .stream()
            .map(ResponseMessage::getMessage)
            .collect(joining(". "));
      } else {
        return t.getClass().getSimpleName() + (t.getMessage() == null ? "" : ": " + t.getMessage());
      }
    } else {
      return t.getClass().getSimpleName() + (t.getMessage() == null ? "" : ": " + t.getMessage());
    }
  }
}
