package software.wings.exception;

import static io.harness.eraro.Level.ERROR;
import static io.harness.govern.Switch.unhandled;
import static java.util.stream.Collectors.joining;
import static software.wings.beans.ResponseMessage.aResponseMessage;
import static software.wings.exception.WingsException.ReportTarget.DELEGATE_LOG_SYSTEM;
import static software.wings.exception.WingsException.ReportTarget.LOG_SYSTEM;
import static software.wings.exception.WingsException.ReportTarget.RED_BELL_ALERT;
import static software.wings.exception.WingsException.ReportTarget.REST_API;

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
    // Logging system.
    LOG_SYSTEM,

    // Logging system.
    DELEGATE_LOG_SYSTEM,

    // When exception targets user it will be serialized in the rest APIs
    REST_API,

    // When exception targets user admin it will trigger an alert in the harness app.
    RED_BELL_ALERT,
  }

  public static final ReportTarget[] EVERYBODY = {LOG_SYSTEM, REST_API, RED_BELL_ALERT};
  public static final ReportTarget[] ADMIN_SRE = {LOG_SYSTEM, DELEGATE_LOG_SYSTEM, RED_BELL_ALERT};
  public static final ReportTarget[] USER_SRE = {LOG_SYSTEM, DELEGATE_LOG_SYSTEM, REST_API};
  public static final ReportTarget[] USER_ADMIN = {DELEGATE_LOG_SYSTEM, RED_BELL_ALERT, REST_API};
  public static final ReportTarget[] ADMIN = {DELEGATE_LOG_SYSTEM, RED_BELL_ALERT};
  public static final ReportTarget[] SRE = {LOG_SYSTEM, DELEGATE_LOG_SYSTEM};
  public static final ReportTarget[] USER = {REST_API};
  public static final ReportTarget[] NOBODY = {};

  private ReportTarget[] reportTargets = USER_SRE;

  public enum ExecutionContext { MANAGER, DELEGATE }

  /**
   * The Response message list.
   */
  private ResponseMessage responseMessage;

  private Map<String, Object> params = new HashMap<>();

  public WingsException(String message) {
    this(ErrorCode.UNKNOWN_ERROR, message);
  }

  public WingsException(String message, ReportTarget[] reportTargets) {
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

  public WingsException(ErrorCode errorCode, String message, ReportTarget[] reportTargets) {
    this(errorCode, message, reportTargets, (Throwable) null);
  }

  public WingsException(ErrorCode errorCode, ReportTarget[] reportTargets, Throwable cause) {
    this(errorCode, null, reportTargets, cause);
  }

  public WingsException(ErrorCode errorCode, String message, ReportTarget[] reportTargets, Throwable cause) {
    this(errorCode, message, cause);
    this.reportTargets = reportTargets;
  }

  public WingsException(ErrorCode errorCode) {
    this(errorCode, (Throwable) null);
  }

  public WingsException(ErrorCode errorCode, ReportTarget[] reportTargets) {
    this(errorCode, (Throwable) null);
    this.reportTargets = reportTargets;
  }

  public WingsException(ErrorCode errorCode, Throwable cause) {
    this(errorCode, (String) null, cause);
  }

  public WingsException(ErrorCode errorCode, String message, Throwable cause) {
    super(message == null ? errorCode.name() : message, cause);
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

  public void logProcessedMessages(ExecutionContext context, Logger logger) {
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

    final List<ResponseMessage> responseMessages = getResponseMessageList(target);

    String msg = "Exception occurred: " + getMessage();
    String messages = responseMessages.stream().map(ResponseMessage::getMessage).collect(joining(". "));
    if (responseMessages.stream().anyMatch(responseMessage -> responseMessage.getLevel() == ERROR)) {
      logger.error(messages);
      logger.error(msg, this);
    } else {
      logger.info(messages);
      if (logger.isDebugEnabled()) {
        logger.debug(msg, this);
      }
    }
  }
}
