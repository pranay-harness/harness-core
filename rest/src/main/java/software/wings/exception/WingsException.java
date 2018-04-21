package software.wings.exception;

import static java.util.stream.Collectors.joining;
import static software.wings.beans.ResponseMessage.Level.ERROR;
import static software.wings.beans.ResponseMessage.Level.INFO;
import static software.wings.beans.ResponseMessage.Level.WARN;
import static software.wings.beans.ResponseMessage.aResponseMessage;
import static software.wings.exception.WingsException.ReportTarget.HARNESS_ENGINEER;
import static software.wings.exception.WingsException.ReportTarget.USER;
import static software.wings.exception.WingsException.ReportTarget.USER_ADMIN;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;
import software.wings.common.cache.ResponseCodeCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * The generic exception class for the Wings Application.
 *
 * @author Rishi
 */
@Getter
public class WingsException extends WingsApiException {
  private static final long serialVersionUID = -3266129015976960503L;

  public enum ReportTarget {
    // When exception targets harness engineer it will be logged.
    HARNESS_ENGINEER,

    // When exception targets user it will be serialized in the rest APIs
    USER,

    // When exception targets user admin it will trigger an alert in the harness app.
    USER_ADMIN,
  }

  public static final ReportTarget[] SERIOUS = {HARNESS_ENGINEER, USER};
  public static final ReportTarget[] ALERTING = {USER_ADMIN, USER};
  public static final ReportTarget[] HARMLESS = {USER};
  public static final ReportTarget[] IGNORABLE = {};

  @Builder.Default private ReportTarget[] reportTargets = SERIOUS;

  /**
   * The Response message list.
   */
  private ResponseMessage responseMessage;

  private Map<String, Object> params = new HashMap<>();

  public WingsException(String message) {
    this(ErrorCode.UNKNOWN_ERROR, message);
  }

  public WingsException(String message, ReportTarget... reportTargets) {
    this(ErrorCode.UNKNOWN_ERROR, message);
    this.reportTargets = reportTargets;
  }

  public WingsException(String message, Throwable cause) {
    this(ErrorCode.UNKNOWN_ERROR, message, cause);
  }

  public WingsException(Throwable cause) {
    this(ErrorCode.UNKNOWN_ERROR, cause);
  }

  public WingsException(ErrorCode errorCode, String message) {
    this(errorCode, message, (Throwable) null);
  }

  public WingsException(ErrorCode errorCode, String message, ReportTarget... reportTargets) {
    this(errorCode, message, (Throwable) null);
    this.reportTargets = reportTargets;
  }

  public WingsException(ErrorCode errorCode) {
    this(errorCode, (Throwable) null);
  }

  public WingsException(ErrorCode errorCode, ReportTarget... reportTargets) {
    this(errorCode, (Throwable) null);
    this.reportTargets = reportTargets;
  }

  public WingsException(ErrorCode errorCode, Throwable cause) {
    this(errorCode, null, cause);
  }

  public WingsException(ErrorCode errorCode, String message, Throwable cause) {
    super(message == null ? errorCode.getCode() : message, cause);
    responseMessage = aResponseMessage().code(errorCode).message(message).build();
  }

  /**
   * Instantiates a new wings exception.
   *
   * @param params    the params
   * @param errorCode the error code
   */
  public WingsException(Map<String, Object> params, ErrorCode errorCode) {
    this(errorCode, (Throwable) null);
    this.params = params;
  }

  public WingsException(@NotNull ResponseMessage responseMessage) {
    this(responseMessage, null);
  }

  public WingsException(@NotNull ResponseMessage responseMessage, Throwable cause) {
    super(
        responseMessage.getMessage() == null ? responseMessage.getCode().name() : responseMessage.getMessage(), cause);
    this.responseMessage = responseMessage;
  }

  public List<ResponseMessage> getResponseMessageList(ReportTarget reportTarget) {
    List<ResponseMessage> list = new ArrayList<>();
    for (Throwable ex = this; ex != null; ex = ex.getCause()) {
      if (!(ex instanceof WingsException)) {
        continue;
      }
      final WingsException exception = (WingsException) ex;
      if (!ArrayUtils.contains(exception.getReportTargets(), reportTarget)) {
        continue;
      }

      ResponseMessage responseMessage =
          ResponseCodeCache.getInstance().rebuildMessage(exception.getResponseMessage(), exception.getParams());
      list.add(responseMessage);
    }

    return list;
  }

  public WingsException addParam(String key, Object value) {
    params.put(key, value);
    return this;
  }

  public void excludeReportTarget(ErrorCode code, ReportTarget target) {
    if (responseMessage.getCode() == code) {
      reportTargets = ArrayUtils.removeElement(reportTargets, target);
    }

    Throwable cause = getCause();

    while (cause != null) {
      if (cause instanceof WingsException) {
        ((WingsException) cause).excludeReportTarget(code, target);

        // the cause exception will take care of its cause exception. There is no need for this function to keep going.
        break;
      }

      cause = cause.getCause();
    }
  }

  public void logProcessedMessages(Logger logger) {
    final List<ResponseMessage> responseMessages = getResponseMessageList(HARNESS_ENGINEER);

    String msg = "Exception occurred: " + getMessage();
    String responseMsgs = responseMessages.stream().map(ResponseMessage::getMessage).collect(joining(". "));
    if (responseMessages.stream().anyMatch(responseMessage -> responseMessage.getLevel() == ERROR)) {
      logger.error(msg, this);
      logger.error(responseMsgs);
    } else if (responseMessages.stream().anyMatch(responseMessage -> responseMessage.getLevel() == WARN)) {
      logger.warn(msg, this);
      logger.warn(responseMsgs);
    } else if (responseMessages.stream().anyMatch(responseMessage -> responseMessage.getLevel() == INFO)) {
      logger.info(msg, this);
      logger.info(responseMsgs);
    } else {
      logger.debug(msg, this);
      logger.debug(responseMsgs);
    }
  }
}
