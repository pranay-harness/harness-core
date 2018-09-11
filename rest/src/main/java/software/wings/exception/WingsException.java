package software.wings.exception;

import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.eraro.ErrorCode.UNKNOWN_ERROR;
import static software.wings.exception.WingsException.ReportTarget.DELEGATE_LOG_SYSTEM;
import static software.wings.exception.WingsException.ReportTarget.LOG_SYSTEM;
import static software.wings.exception.WingsException.ReportTarget.RED_BELL_ALERT;
import static software.wings.exception.WingsException.ReportTarget.REST_API;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import lombok.Builder;
import lombok.Getter;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * The generic exception class for the Wings Application.
 *
 * @author Rishi
 */
@Getter
public class WingsException extends RuntimeException {
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

  public enum ExecutionContext { MANAGER, DELEGATE }

  private EnumSet<ReportTarget> reportTargets = USER_SRE;

  private ErrorCode code = DEFAULT_ERROR_CODE;
  private Level level = Level.ERROR;

  private Map<String, Object> params = new HashMap<>();

  private Map<Class, Object> contextObjects = new HashMap<>();

  @Builder
  public WingsException(
      String message, Throwable cause, ErrorCode code, Level level, EnumSet<ReportTarget> reportTargets) {
    super(message == null ? code.name() : message, cause);
    this.code = code == null ? UNKNOWN_ERROR : code;
    this.level = level == null ? Level.ERROR : level;
    this.reportTargets = reportTargets == null ? USER_SRE : reportTargets;
  }

  public WingsException(String message) {
    this(UNKNOWN_ERROR, message);
  }

  public WingsException(String message, EnumSet<ReportTarget> reportTargets) {
    this(UNKNOWN_ERROR, message);
    this.reportTargets = reportTargets.clone();
  }
  public WingsException(String message, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    this(UNKNOWN_ERROR, message, cause);
    this.reportTargets = reportTargets.clone();
  }

  public WingsException(String message, Throwable cause) {
    this(UNKNOWN_ERROR, message, cause);
  }

  public WingsException(Throwable cause) {
    this(UNKNOWN_ERROR, cause);
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
    code = errorCode;
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

  public <T> WingsException addContext(Class<?> clz, T object) {
    contextObjects.put(clz, object);
    return this;
  }

  public WingsException addParam(String key, Object value) {
    params.put(key, value);
    return this;
  }

  public void excludeReportTarget(ErrorCode errorCode, ReportTarget target) {
    if (code == errorCode) {
      reportTargets.remove(target);
    }

    Throwable cause = getCause();

    while (cause != null) {
      if (cause instanceof WingsException) {
        ((WingsException) cause).excludeReportTarget(errorCode, target);

        // the cause exception will take care of its cause exception. There is no need for this function to keep going.
        break;
      }

      cause = cause.getCause();
    }
  }
}
