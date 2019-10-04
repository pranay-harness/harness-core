package io.harness.logging;

import static io.harness.eraro.Level.ERROR;
import static io.harness.exception.WingsException.ReportTarget.DELEGATE_LOG_SYSTEM;
import static io.harness.exception.WingsException.ReportTarget.LOG_SYSTEM;
import static io.harness.exception.WingsException.ReportTarget.UNIVERSAL;
import static io.harness.govern.Switch.unhandled;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static java.util.stream.Collectors.joining;

import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCodeName;
import io.harness.eraro.MessageManager;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.WingsException;
import io.harness.exception.WingsException.ExecutionContext;
import io.harness.exception.WingsException.ReportTarget;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@UtilityClass
public class ExceptionLogger {
  protected static String calculateResponseMessage(List<ResponseMessage> responseMessages) {
    return "Response message: "
        + responseMessages.stream().map(ResponseMessage::getMessage).collect(joining("\n                  "));
  }

  protected static String calculateErrorMessage(WingsException exception, List<ResponseMessage> responseMessages) {
    return Stream.of(calculateResponseMessage(responseMessages), "Exception occurred: " + exception.getMessage())
        .filter(EmptyPredicate::isNotEmpty)
        .collect(joining("\n"));
  }

  protected static String calculateInfoMessage(List<ResponseMessage> responseMessages) {
    return calculateResponseMessage(responseMessages);
  }

  protected static String calculateDebugMessage(WingsException exception) {
    return "Exception occurred: " + exception.getMessage();
  }

  public static List<ResponseMessage> getResponseMessageList(WingsException wingsException, ReportTarget reportTarget) {
    List<ResponseMessage> list = new ArrayList<>();
    for (Throwable ex = wingsException; ex != null; ex = ex.getCause()) {
      if (!(ex instanceof WingsException)) {
        continue;
      }
      final WingsException exception = (WingsException) ex;
      if (reportTarget != UNIVERSAL && !exception.getReportTargets().contains(reportTarget)) {
        continue;
      }
      final String message =
          MessageManager.getInstance().prepareMessage(ErrorCodeName.builder().value(exception.getCode().name()).build(),
              exception.getMessage(), exception.getParams());
      ResponseMessage responseMessage =
          ResponseMessage.builder().code(exception.getCode()).message(message).level(exception.getLevel()).build();

      ResponseMessage finalResponseMessage = responseMessage;
      if (list.stream().noneMatch(msg -> StringUtils.equals(finalResponseMessage.getMessage(), msg.getMessage()))) {
        list.add(responseMessage);
      }
    }

    return list;
  }

  public static void logProcessedMessages(WingsException exception, ExecutionContext context, Logger logger) {
    Exception processedException = null;
    try (AutoLogContext ignore = new AutoLogContext(exception.calcRecursiveContextObjects(), OVERRIDE_ERROR)) {
      ReportTarget target = UNIVERSAL;

      switch (context) {
        case MANAGER:
          target = LOG_SYSTEM;
          break;
        case DELEGATE:
          target = DELEGATE_LOG_SYSTEM;
          break;
        default:
          unhandled(context);
      }

      List<ResponseMessage> responseMessages = getResponseMessageList(exception, target);
      if (responseMessages.stream().anyMatch(responseMessage -> responseMessage.getLevel() == ERROR)) {
        if (logger.isErrorEnabled()) {
          logger.error(calculateErrorMessage(exception, responseMessages), exception);
        }
      } else {
        responseMessages = getResponseMessageList(exception, UNIVERSAL);
        if (logger.isInfoEnabled()) {
          logger.info(calculateInfoMessage(responseMessages));
          logger.info(calculateDebugMessage(exception), exception);
        }
      }
    } catch (Exception e) {
      processedException = e;
      logger.error("Original exception:", exception);
    }

    if (processedException != null) {
      logger.error("Error processing messages.", processedException);
    }
  }
}
