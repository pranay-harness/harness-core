package software.wings.exception;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.Level.ERROR;
import static io.harness.govern.Switch.unhandled;
import static java.util.stream.Collectors.joining;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static software.wings.beans.ResponseMessage.aResponseMessage;
import static software.wings.beans.RestResponse.Builder.aRestResponse;
import static software.wings.exception.WingsException.ExecutionContext.MANAGER;
import static software.wings.exception.WingsException.ReportTarget.DELEGATE_LOG_SYSTEM;
import static software.wings.exception.WingsException.ReportTarget.LOG_SYSTEM;
import static software.wings.exception.WingsException.ReportTarget.REST_API;
import static software.wings.exception.WingsException.ReportTarget.UNIVERSAL;

import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.ErrorCodeName;
import io.harness.eraro.MessageManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ResponseMessage;
import software.wings.exception.WingsException.ExecutionContext;
import software.wings.exception.WingsException.ReportTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

public class WingsExceptionMapper implements ExceptionMapper<WingsException> {
  private static Logger logger = LoggerFactory.getLogger(WingsExceptionMapper.class);

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
          aResponseMessage().code(exception.getCode()).message(message).level(exception.getLevel()).build();

      ResponseMessage finalResponseMessage = responseMessage;
      if (list.stream().noneMatch(msg -> StringUtils.equals(finalResponseMessage.getMessage(), msg.getMessage()))) {
        list.add(responseMessage);
      }
    }

    return list;
  }

  protected static String calculateContextObjectsMessage(WingsException exception) {
    // TODO: use string buffer

    Map<String, Object> context = new TreeMap<>();
    Throwable t = exception;
    while (t != null) {
      if (t instanceof WingsException) {
        ((WingsException) t).getContextObjects().forEach((clz, value) -> context.put(clz.getCanonicalName(), value));
      }
      t = t.getCause();
    }

    if (isEmpty(context)) {
      return null;
    }

    return "Context objects: "
        + context.entrySet()
              .stream()
              .map(entry -> entry.getKey() + ": " + entry.getValue())
              .collect(joining("\n                 "));
  }

  protected static String calculateResponseMessage(List<ResponseMessage> responseMessages) {
    // TODO: use string buffer
    return "Response message: "
        + responseMessages.stream().map(ResponseMessage::getMessage).collect(joining("\n                  "));
  }

  protected static String calculateErrorMessage(WingsException exception, List<ResponseMessage> responseMessages) {
    return Stream
        .of(calculateResponseMessage(responseMessages), calculateContextObjectsMessage(exception),
            "Exception occurred: " + exception.getMessage())
        .filter(EmptyPredicate::isNotEmpty)
        .collect(joining("\n"));
  }

  protected static String calculateInfoMessage(List<ResponseMessage> responseMessages) {
    return calculateResponseMessage(responseMessages);
  }

  protected static String calculateDebugMessage(WingsException exception) {
    return Stream.of(calculateContextObjectsMessage(exception), "Exception occurred: " + exception.getMessage())
        .filter(EmptyPredicate::isNotEmpty)
        .collect(joining("\n"));
  }

  public static void logProcessedMessages(WingsException exception, ExecutionContext context, Logger logger) {
    ReportTarget target = LOG_SYSTEM;

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
      logger.error(calculateErrorMessage(exception, responseMessages), exception);
    } else {
      responseMessages = getResponseMessageList(exception, UNIVERSAL);
      logger.info(calculateInfoMessage(responseMessages));
      logger.info(calculateDebugMessage(exception), exception);
    }
  }

  @Override
  public Response toResponse(WingsException exception) {
    logProcessedMessages(exception, MANAGER, logger);
    List<ResponseMessage> responseMessages = getResponseMessageList(exception, REST_API);

    return Response.status(resolveHttpStatus(responseMessages))
        .entity(aRestResponse().withResponseMessages(responseMessages).build())
        .build();
  }

  private Status resolveHttpStatus(List<ResponseMessage> responseMessageList) {
    ErrorCode errorCode = null;
    if (isNotEmpty(responseMessageList)) {
      errorCode = responseMessageList.get(responseMessageList.size() - 1).getCode();
    }
    if (errorCode != null) {
      return errorCode.getStatus();
    } else {
      return INTERNAL_SERVER_ERROR;
    }
  }
}
