package io.harness.exception;

import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.eraro.ErrorCode.UNKNOWN_ERROR;
import static io.harness.exception.WingsException.ReportTarget.DELEGATE_LOG_SYSTEM;
import static io.harness.exception.WingsException.ReportTarget.GRAPHQL_API;
import static io.harness.exception.WingsException.ReportTarget.LOG_SYSTEM;
import static io.harness.exception.WingsException.ReportTarget.RED_BELL_ALERT;
import static io.harness.exception.WingsException.ReportTarget.REST_API;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The generic exception class for the Wings Application.
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

    GRAPHQL_API
  }

  public static final EnumSet<ReportTarget> EVERYBODY =
      EnumSet.<ReportTarget>of(LOG_SYSTEM, DELEGATE_LOG_SYSTEM, REST_API, RED_BELL_ALERT);
  public static final EnumSet<ReportTarget> ADMIN_SRE =
      EnumSet.<ReportTarget>of(LOG_SYSTEM, DELEGATE_LOG_SYSTEM, RED_BELL_ALERT);
  public static final EnumSet<ReportTarget> USER_SRE =
      EnumSet.<ReportTarget>of(LOG_SYSTEM, DELEGATE_LOG_SYSTEM, REST_API, GRAPHQL_API);
  public static final EnumSet<ReportTarget> USER_ADMIN =
      EnumSet.<ReportTarget>of(DELEGATE_LOG_SYSTEM, RED_BELL_ALERT, REST_API, GRAPHQL_API);
  public static final EnumSet<ReportTarget> ADMIN = EnumSet.<ReportTarget>of(DELEGATE_LOG_SYSTEM, RED_BELL_ALERT);
  public static final EnumSet<ReportTarget> SRE = EnumSet.<ReportTarget>of(LOG_SYSTEM, DELEGATE_LOG_SYSTEM);
  public static final EnumSet<ReportTarget> USER = EnumSet.<ReportTarget>of(REST_API, GRAPHQL_API);
  public static final EnumSet<ReportTarget> GROUP = EnumSet.<ReportTarget>of(REST_API, GRAPHQL_API);
  public static final EnumSet<ReportTarget> NOBODY = EnumSet.noneOf(ReportTarget.class);

  public enum ExecutionContext { MANAGER, DELEGATE }

  private EnumSet<ReportTarget> reportTargets = USER_SRE;

  private ErrorCode code = DEFAULT_ERROR_CODE;
  private Level level = Level.ERROR;

  private Map<String, Object> params = new HashMap<>();

  private Map<String, String> contextObjects;

  private EnumSet<FailureType> failureTypes = EnumSet.noneOf(FailureType.class);

  @Builder
  protected WingsException(String message, Throwable cause, ErrorCode code, Level level,
      EnumSet<ReportTarget> reportTargets, EnumSet<FailureType> failureTypes) {
    super(message == null ? code.name() : message, cause);
    this.code = code == null ? UNKNOWN_ERROR : code;
    this.level = level == null ? Level.ERROR : level;
    this.reportTargets = reportTargets == null ? USER_SRE : reportTargets;
    this.failureTypes = failureTypes == null ? EnumSet.noneOf(FailureType.class) : failureTypes;
    contextObjects = MDC.getCopyOfContextMap();
  }

  @Deprecated
  public static WingsExceptionBuilder builder() {
    return new WingsExceptionBuilder();
  }

  protected static WingsExceptionBuilder internalBuilder() {
    return new WingsExceptionBuilder();
  }

  @Deprecated
  public WingsException(String message) {
    this(UNKNOWN_ERROR, message);
  }

  @Deprecated
  public WingsException(String message, EnumSet<ReportTarget> reportTargets) {
    this(UNKNOWN_ERROR, message);
    this.reportTargets = reportTargets == null ? null : reportTargets.clone();
  }
  @Deprecated
  public WingsException(String message, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    this(UNKNOWN_ERROR, message, cause);
    this.reportTargets = reportTargets == null ? null : reportTargets.clone();
  }

  @Deprecated
  public WingsException(String message, Throwable cause) {
    this(UNKNOWN_ERROR, message, cause);
  }

  @Deprecated
  public WingsException(Throwable cause) {
    this(UNKNOWN_ERROR, cause);
  }

  @Deprecated
  public WingsException(ErrorCode errorCode, String message) {
    this(errorCode, message, (Throwable) null);
  }

  @Deprecated
  public WingsException(ErrorCode errorCode, String message, EnumSet<ReportTarget> reportTargets) {
    this(errorCode, message, reportTargets, (Throwable) null);
  }

  @Deprecated
  public WingsException(ErrorCode errorCode, EnumSet<ReportTarget> reportTargets, Throwable cause) {
    this(errorCode, null, reportTargets, cause);
  }

  @Deprecated
  public WingsException(ErrorCode errorCode, String message, EnumSet<ReportTarget> reportTargets, Throwable cause) {
    this(errorCode, message, cause);
    this.reportTargets = reportTargets == null ? null : reportTargets.clone();
  }

  @Deprecated
  public WingsException(ErrorCode errorCode) {
    this(errorCode, (Throwable) null);
  }

  @Deprecated
  public WingsException(ErrorCode errorCode, EnumSet<ReportTarget> reportTargets) {
    this(errorCode, (Throwable) null);
    this.reportTargets = reportTargets == null ? null : reportTargets.clone();
  }

  @Deprecated
  public WingsException(ErrorCode errorCode, Throwable cause) {
    this(errorCode, (String) null, cause);
  }

  @Deprecated
  public WingsException(ErrorCode errorCode, String message, Throwable cause) {
    super(message == null ? errorCode.name() : message, cause);
    code = errorCode;
  }

  @Deprecated
  public WingsException(Map<String, Object> params, ErrorCode errorCode) {
    this(errorCode, (Throwable) null);
    this.params = params;
  }

  private Map<String, String> getContextObjects() {
    return contextObjects != null ? contextObjects : Collections.emptyMap();
  }

  public Map<String, String> calcRecursiveContextObjects() {
    Map<String, String> result = new HashMap<>();
    Throwable t = this;
    while (t != null) {
      if (t instanceof WingsException) {
        result.putAll(getContextObjects());
      }
      t = t.getCause();
    }

    return result;
  }

  @Deprecated
  public <T> WingsException addContext(Class<?> clz, T object) {
    if (contextObjects == null) {
      contextObjects = new HashMap<>();
    }
    contextObjects.put(clz.getName(), object.toString());
    return this;
  }

  @Deprecated
  // Use param instead, from a helper class
  public WingsException addParam(String key, Object value) {
    params.put(key, value.toString().replace("${", "$ {"));
    return this;
  }

  protected WingsException param(String key, Object value) {
    params.put(key, value.toString().replace("${", "$ {"));
    return this;
  }

  public void excludeReportTarget(ErrorCode errorCode, Set<ReportTarget> targets) {
    if (code == errorCode) {
      if (targets != null) {
        reportTargets.removeAll(targets);
      }
    }
    Throwable cause = getCause();

    while (cause != null) {
      if (cause instanceof WingsException) {
        ((WingsException) cause).excludeReportTarget(errorCode, targets);

        // the cause exception will take care of its cause exception. There is no need for this function to keep going.
        break;
      }

      cause = cause.getCause();
    }
  }
}
