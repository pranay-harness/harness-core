package io.harness.exception;

import static io.harness.exception.WingsException.ReportTarget.REST_API;
import static java.util.stream.Collectors.joining;

import io.harness.eraro.ResponseMessage;
import io.harness.logging.ExceptionLogger;

import javax.validation.ConstraintViolationException;

public class ExceptionUtils {
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
