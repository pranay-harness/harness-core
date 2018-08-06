package software.wings.exception;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.Level.ERROR;
import static io.harness.govern.Switch.unhandled;
import static java.util.stream.Collectors.joining;
import static software.wings.beans.ResponseMessage.aResponseMessage;
import static software.wings.exception.WingsException.ReportTarget.DELEGATE_LOG_SYSTEM;
import static software.wings.exception.WingsException.ReportTarget.LOG_SYSTEM;
import static software.wings.exception.WingsException.ReportTarget.RED_BELL_ALERT;
import static software.wings.exception.WingsException.ReportTarget.REST_API;
import static software.wings.exception.WingsException.ReportTarget.UNIVERSAL;

import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCodeName;
import io.harness.eraro.MessageManager;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;

/**
 * The generic exception class for the Wings Application.
 *
 * @author Rishi
 */
@Getter
public class WingsException extends RuntimeException {
  private static final long serialVersionUID = -3266129015976960503L;

  public enum ReportTarget {
    // Universal
    UNIVERSAL,

    // Logging system.
    LOG_SYSTEM,

    // Logging system.
    DELEGATE_LOG_SYSTEM,

    // When exception targets user it will be serialized in the rest APIs
    REST_API,

    // When exception targets user admin it will trigger an alert in the harness app.
    RED_BELL_ALERT,
  }

  public static final EnumSet<ReportTarget> EVERYBODY =
      EnumSet.<ReportTarget>of(LOG_SYSTEM, DELEGATE_LOG_SYSTEM, REST_API, RED_BELL_ALERT);
  public static final EnumSet<ReportTarget> ADMIN_SRE =
      EnumSet.<ReportTarget>of(LOG_SYSTEM, DELEGATE_LOG_SYSTEM, RED_BELL_ALERT);
  public static final EnumSet<ReportTarget> USER_SRE =
      EnumSet.<ReportTarget>of(LOG_SYSTEM, DELEGATE_LOG_SYSTEM, REST_API);
  public static final EnumSet<ReportTarget> USER_ADMIN =
      EnumSet.<ReportTarget>of(DELEGATE_LOG_SYSTEM, RED_BELL_ALERT, REST_API);
  public static final EnumSet<ReportTarget> ADMIN = EnumSet.<ReportTarget>of(DELEGATE_LOG_SYSTEM, RED_BELL_ALERT);
  public static final EnumSet<ReportTarget> SRE = EnumSet.<ReportTarget>of(LOG_SYSTEM, DELEGATE_LOG_SYSTEM);
  public static final EnumSet<ReportTarget> USER = EnumSet.<ReportTarget>of(REST_API);
  public static final EnumSet<ReportTarget> NOBODY = EnumSet.noneOf(ReportTarget.class);
  private EnumSet<ReportTarget> reportTargets = USER_SRE;

  public enum ExecutionContext { MANAGER, DELEGATE }

  /**
   * The Response message list.
   */
  private ResponseMessage responseMessage;

  private Map<String, Object> params = new HashMap<>();

  private Map<Class, Object> contextObjects = new HashMap<>();

  public WingsException(String message) {
    this(ErrorCode.UNKNOWN_ERROR, message);
  }

  public WingsException(String message, EnumSet<ReportTarget> reportTargets) {
    this(ErrorCode.UNKNOWN_ERROR, message);
    this.reportTargets = reportTargets.clone();
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

  public WingsException(ErrorCode errorCode, String message, EnumSet<ReportTarget> reportTargets) {
    this(errorCode, message, reportTargets, (Throwable) null);
  }

  public WingsException(ErrorCode errorCode, EnumSet<ReportTarget> reportTargets, Throwable cause) {
    this(errorCode, null, reportTargets, cause);
  }

  public WingsException(ErrorCode errorCode, String message, EnumSet<ReportTarget> reportTargets, Throwable cause) {
    this(errorCode, message, cause);
    this.reportTargets = reportTargets.clone();
  }

  public WingsException(ErrorCode errorCode) {
    this(errorCode, (Throwable) null);
  }

  public WingsException(ErrorCode errorCode, EnumSet<ReportTarget> reportTargets) {
    this(errorCode, (Throwable) null);
    this.reportTargets = reportTargets.clone();
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

  public <T> WingsException addContext(Class<?> clz, T object) {
    contextObjects.put(clz, object);
    return this;
  }

  public List<ResponseMessage> getResponseMessageList(ReportTarget reportTarget) {
    List<ResponseMessage> list = new ArrayList<>();
    for (Throwable ex = this; ex != null; ex = ex.getCause()) {
      if (!(ex instanceof WingsException)) {
        continue;
      }
      final WingsException exception = (WingsException) ex;
      if (reportTarget != UNIVERSAL && !exception.getReportTargets().contains(reportTarget)) {
        continue;
      }
      ResponseMessage responseMessage = exception.getResponseMessage();
      if (isEmpty(responseMessage.getMessage())) {
        final String message = MessageManager.getInstance().prepareMessage(
            ErrorCodeName.builder().value(responseMessage.getCode().name()).build(), params);
        responseMessage = aResponseMessage()
                              .code(responseMessage.getCode())
                              .message(message)
                              .level(responseMessage.getLevel())
                              .build();
      }

      ResponseMessage finalResponseMessage = responseMessage;
      if (list.stream().noneMatch(msg -> StringUtils.equals(finalResponseMessage.getMessage(), msg.getMessage()))) {
        list.add(responseMessage);
      }
    }

    return list;
  }

  public WingsException addParam(String key, Object value) {
    params.put(key, value);
    return this;
  }

  public void excludeReportTarget(ErrorCode code, ReportTarget target) {
    if (responseMessage.getCode() == code) {
      reportTargets.remove(target);
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

  protected String calculateContextObjectsMessage() {
    // TODO: use string buffer

    Map<String, Object> context = new TreeMap<>();
    Throwable t = this;
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

  protected String calculateResponseMessage(List<ResponseMessage> responseMessages) {
    // TODO: use string buffer
    return "Response message: "
        + responseMessages.stream().map(ResponseMessage::getMessage).collect(joining("\n                  "));
  }

  protected String calculateErrorMessage(List<ResponseMessage> responseMessages) {
    return Stream
        .of(calculateResponseMessage(responseMessages), calculateContextObjectsMessage(),
            "Exception occurred: " + getMessage())
        .filter(EmptyPredicate::isNotEmpty)
        .collect(joining("\n"));
  }

  protected String calculateInfoMessage(List<ResponseMessage> responseMessages) {
    return calculateResponseMessage(responseMessages);
  }

  protected String calculateDebugMessage() {
    return Stream.of(calculateContextObjectsMessage(), "Exception occurred: " + getMessage())
        .filter(EmptyPredicate::isNotEmpty)
        .collect(joining("\n"));
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

    List<ResponseMessage> responseMessages = getResponseMessageList(target);
    if (responseMessages.stream().anyMatch(responseMessage -> responseMessage.getLevel() == ERROR)) {
      logger.error(calculateErrorMessage(responseMessages), this);
    } else {
      responseMessages = getResponseMessageList(UNIVERSAL);
      logger.info(calculateInfoMessage(responseMessages));
      if (logger.isDebugEnabled()) {
        logger.debug(calculateDebugMessage(), this);
      }
    }
  }
}
