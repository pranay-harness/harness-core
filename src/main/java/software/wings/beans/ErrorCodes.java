package software.wings.beans;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import javax.ws.rs.core.Response.Status;

public enum ErrorCodes {
  DEFAULT_ERROR_CODE("DEFAULT_ERROR_CODE"),
  INVALID_ARGUMENT("INVALID_ARGUMENT"),
  INVALID_TOKEN("INVALID_TOKEN", UNAUTHORIZED),
  EXPIRED_TOKEN("EXPIRED_TOKEN", UNAUTHORIZED),
  ACCESS_DENIED("ACCESS_DENIED", FORBIDDEN),

  INVALID_CREDENTIAL("INVALID_CREDENTIAL_ERROR", UNAUTHORIZED),
  INVALID_KEY("INVALID_KEY_ERROR"),
  INVALID_KEYPATH("INVALID_KEYPATH_ERROR"),
  UNKNOWN_HOST("UNKNOWN_HOST_ERROR"),
  UNREACHABLE_HOST("UNREACHABLE_HOST_ERROR"),
  INVALID_PORT("INVALID_OR_BLOCKED_PORT_ERROR"),
  SSH_SESSION_TIMEOUT("SSH_SESSION_TIMEOUT"),
  SOCKET_CONNECTION_ERROR("SSH_SOCKET_CONNECTION_ERROR"),
  SOCKET_CONNECTION_TIMEOUT("SOCKET_CONNECTION_TIMEOUT_ERROR"),
  UNKNOWN_ERROR("UNKNOWN_ERROR"),
  UNKNOWN_EXECUTOR_TYPE_ERROR("UNKNOWN_EXECUTOR_TYPE_ERROR"),

  DUPLICATE_STATE_NAMES("DUPLICATE_STATE_NAMES"),
  TRANSITION_NOT_LINKED("TRANSITION_NOT_LINKED"),
  TRANSITION_TO_INCORRECT_STATE("TRANSITION_TO_INCORRECT_STATE"),
  TRANSITION_TYPE_NULL("TRANSITION_TYPE_NULL"),
  STATES_WITH_DUP_TRANSITIONS("STATES_WITH_DUP_TRANSITIONS"),
  NON_FORK_STATES("NON_FORK_STATES"),
  NON_REPEAT_STATES("NON_REPEAT_STATES"),
  INITIAL_STATE_NOT_DEFINED("INITIAL_STATE_NOT_DEFINED"),
  FILE_INTEGRITY_CHECK_FAILED("FILE_INTEGRITY_CHECK_FAILED"),
  INVALID_URL("INVALID_URL"),
  FILE_DOWNLOAD_FAILED("FILE_DOWNLOAD_FAILED"),
  PLATFORM_SOFTWARE_DELETE_ERROR("PLATFORM_SOFTWARE_DELETE_ERROR"),
  INVALID_CSV_FILE("INVALID_CSV_FILE"),
  INVALID_REQUEST("INVALID_REQUEST"),
  PIPELINE_ALREADY_TRIGGERED("PIPELINE_ALREADY_TRIGGERED"),
  NON_EXISTING_PIPELINE("NON_EXISTING_PIPELINE"),

  DUPLICATE_COMMAND_NAMES("DUPLICATE_COMMAND_NAMES"),
  INVALID_PIPELINE("INVALID_PIPELINE");

  public static final String ARGS_NAME = "ARGS_NAME";
  private String code;
  private Status status = BAD_REQUEST;

  ErrorCodes(String code) {
    this.code = code;
  }

  ErrorCodes(String code, Status status) {
    this.code = code;
    this.status = status;
  }

  public String getCode() {
    return code;
  }

  public Status getStatus() {
    return status;
  }
}
