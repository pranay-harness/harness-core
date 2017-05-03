package software.wings.beans;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.GATEWAY_TIMEOUT;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import com.google.common.base.Splitter;

import java.util.stream.Collectors;
import javax.ws.rs.core.Response.Status;

/**
 * The enum Error codes.
 */
public enum ErrorCode {
  /**
   * Default error code error codes.
   */
  DEFAULT_ERROR_CODE("DEFAULT_ERROR_CODE"),

  /**
   * Invalid argument error codes.
   */
  INVALID_ARGUMENT("INVALID_ARGUMENT"),

  /**
   * Invalid email sent for registration.
   */
  INVALID_EMAIL("INVALID_EMAIL"),

  /**
   * Domain not allowed to register error codes.
   */
  DOMAIN_NOT_ALLOWED_TO_REGISTER("DOMAIN_NOT_ALLOWED_TO_REGISTER"),

  /**
   * User already registered error codes.
   */
  USER_ALREADY_REGISTERED("USER_ALREADY_REGISTERED", CONFLICT),

  /**
   * User invitation does not exist error code.
   */
  USER_INVITATION_DOES_NOT_EXIST("USER_INVITATION_DOES_NOT_EXIST", UNAUTHORIZED, "User not invited to access account"),

  /**
   * User does not exist error codes.
   */
  USER_DOES_NOT_EXIST("USER_DOES_NOT_EXIST", UNAUTHORIZED),

  /**
   * User domain not allowed.
   */
  USER_DOMAIN_NOT_ALLOWED("USER_DOMAIN_NOT_ALLOWED", UNAUTHORIZED),

  /**
   * Role does not exist error codes.
   */
  ROLE_DOES_NOT_EXIST("ROLE_DOES_NOT_EXIST", UNAUTHORIZED),

  /**
   * Email not verified error codes.
   */
  EMAIL_NOT_VERIFIED("EMAIL_NOT_VERIFIED", UNAUTHORIZED),

  /**
   * Email verification token not found error codes.
   */
  EMAIL_VERIFICATION_TOKEN_NOT_FOUND("EMAIL_VERIFICATION_TOKEN_NOT_FOUND", NOT_FOUND),

  /**
   * Invalid token error codes.
   */
  INVALID_TOKEN("INVALID_TOKEN", UNAUTHORIZED),

  /**
   * Expired token error codes.
   */
  EXPIRED_TOKEN("EXPIRED_TOKEN", UNAUTHORIZED),

  /**
   * Access denied error codes.
   */
  ACCESS_DENIED("ACCESS_DENIED", FORBIDDEN),

  /**
   * Invalid credential error codes.
   */
  INVALID_CREDENTIAL("INVALID_CREDENTIAL", UNAUTHORIZED),

  /**
   * Invalid key error codes.
   */
  INVALID_KEY("INVALID_KEY", "Invalid key"),

  /**
   * Invalid keypath error codes.
   */
  INVALID_KEYPATH("INVALID_KEYPATH"),

  /**
   * Unknown host error codes.
   */
  UNKNOWN_HOST("UNKNOWN_HOST", "Invalid hostname"),

  /**
   * Unreachable host error codes.
   */
  UNREACHABLE_HOST("UNREACHABLE_HOST", "Unreachable hostname"),

  /**
   * Invalid port error codes.
   */
  INVALID_PORT("INVALID_PORT"),

  /**
   * Ssh session timeout error codes.
   */
  SSH_SESSION_TIMEOUT("SSH_SESSION_TIMEOUT"),

  /**
   * Socket connection error error codes.
   */
  SOCKET_CONNECTION_ERROR("SOCKET_CONNECTION_ERROR", "Connection error"),

  /**
   * Socket connection timeout error codes.
   */
  SOCKET_CONNECTION_TIMEOUT("SOCKET_CONNECTION_TIMEOUT", "Connection timeout"),

  /**
   * Invalid execution id error codes.
   */
  INVALID_EXECUTION_ID("INVALID_EXECUTION_ID"),

  /**
   * Error in getting channel streams error codes.
   */
  ERROR_IN_GETTING_CHANNEL_STREAMS("ERROR_IN_GETTING_CHANNEL_STREAMS"),

  /**
   * Unknown error error codes.
   */
  UNKNOWN_ERROR("UNKNOWN_ERROR"),

  /**
   * Unknown executor type error error codes.
   */
  UNKNOWN_EXECUTOR_TYPE_ERROR("UNKNOWN_EXECUTOR_TYPE_ERROR"),

  /**
   * Duplicate state names error codes.
   */
  DUPLICATE_STATE_NAMES("DUPLICATE_STATE_NAMES"),

  /**
   * Transition not linked error codes.
   */
  TRANSITION_NOT_LINKED("TRANSITION_NOT_LINKED"),

  /**
   * Transition to incorrect state error codes.
   */
  TRANSITION_TO_INCORRECT_STATE("TRANSITION_TO_INCORRECT_STATE"),

  /**
   * Transition type null error codes.
   */
  TRANSITION_TYPE_NULL("TRANSITION_TYPE_NULL"),

  /**
   * States with dup transitions error codes.
   */
  STATES_WITH_DUP_TRANSITIONS("STATES_WITH_DUP_TRANSITIONS"),

  /**
   * Non fork states error codes.
   */
  NON_FORK_STATES("NON_FORK_STATES"),

  /**
   * Non repeat states error codes.
   */
  NON_REPEAT_STATES("NON_REPEAT_STATES"),

  /**
   * Initial state not defined error codes.
   */
  INITIAL_STATE_NOT_DEFINED("INITIAL_STATE_NOT_DEFINED"),

  /**
   * File integrity check failed error codes.
   */
  FILE_INTEGRITY_CHECK_FAILED("FILE_INTEGRITY_CHECK_FAILED"),

  /**
   * Invalid url error codes.
   */
  INVALID_URL("INVALID_URL"),

  /**
   * File download failed error codes.
   */
  FILE_DOWNLOAD_FAILED("FILE_DOWNLOAD_FAILED"),

  /**
   * Platform software delete error error codes.
   */
  PLATFORM_SOFTWARE_DELETE_ERROR("PLATFORM_SOFTWARE_DELETE_ERROR"),

  /**
   * Invalid csv file error codes.
   */
  INVALID_CSV_FILE("INVALID_CSV_FILE"),

  /**
   * Invalid request error codes.
   */
  INVALID_REQUEST("INVALID_REQUEST"),

  /**
   * Pipeline already triggered error codes.
   */
  PIPELINE_ALREADY_TRIGGERED("PIPELINE_ALREADY_TRIGGERED"),

  /**
   * Non existing pipeline error codes.
   */
  NON_EXISTING_PIPELINE("NON_EXISTING_PIPELINE"),

  /**
   * Duplicate command names error codes.
   */
  DUPLICATE_COMMAND_NAMES("DUPLICATE_COMMAND_NAMES"),

  /**
   * Invalid pipeline error codes.
   */
  INVALID_PIPELINE("INVALID_PIPELINE"),

  /**
   * Command does not exist error codes.
   */
  COMMAND_DOES_NOT_EXIST("COMMAND_DOES_NOT_EXIST"),

  /**
   * Duplicate artifact source names error codes.
   */
  DUPLICATE_ARTIFACTSTREAM_NAMES("DUPLICATE_ARTIFACTSTREAM_NAMES"),

  /**
   * Duplicate host names error codes.
   */
  DUPLICATE_HOST_NAMES("DUPLICATE_HOST_NAMES"), /**
                                                 * State not for resume error codes.
                                                 */
  STATE_NOT_FOR_RESUME("STATE_NOT_FOR_RESUME"), /**
                                                 * State not for abort error codes.
                                                 */
  STATE_NOT_FOR_ABORT("STATE_NOT_FOR_ABORT"), /**
                                               * State abort failed error codes.
                                               */
  STATE_ABORT_FAILED("STATE_ABORT_FAILED"), /**
                                             * State pause failed error codes.
                                             */
  STATE_PAUSE_FAILED("STATE_PAUSE_FAILED"), /**
                                             * State not for pause error codes.
                                             */
  STATE_NOT_FOR_PAUSE("STATE_NOT_FOR_PAUSE"), /**
                                               * State not for retry error codes.
                                               */
  STATE_NOT_FOR_RETRY("STATE_NOT_FOR_RETRY"), /**
                                               * Pause all already error codes.
                                               */
  PAUSE_ALL_ALREADY("PAUSE_ALL_ALREADY"), /**
                                           * Resume all already error codes.
                                           */
  RESUME_ALL_ALREADY("RESUME_ALL_ALREADY"), /**
                                             * Abort all already error codes.
                                             */
  ABORT_ALL_ALREADY("ABORT_ALL_ALREADY"),

  /**
   * Retry failed error codes.
   */
  RETRY_FAILED("RETRY_FAILED"), /**
                                 * Unknown artifact type error codes.
                                 */
  UNKNOWN_ARTIFACT_TYPE("UNKNOWN_ARTIFACT_TYPE"), /**
                                                   * Timeout error codes.
                                                   */
  INIT_TIMEOUT("INIT_TIMEOUT"),

  /**
   * License expired error code.
   */
  LICENSE_EXPIRED("LICENSE_EXPIRED"),

  /**
   * Not licensed error code.
   */
  NOT_LICENSED("NOT_LICENSED"), /**
                                 * Request timeout error code.
                                 */
  REQUEST_TIMEOUT("REQUEST_TIMEOUT", GATEWAY_TIMEOUT),

  /**
   * Workflow already triggered
   */
  WORKFLOW_ALREADY_TRIGGERED("WORKFLOW_ALREADY_TRIGGERED"),

  /**
   * Jenkins response error
   */
  JENKINS_ERROR("JENKINS_ERROR"),

  /**
   * Invalid Artifact Source
   */
  INVALID_ARTIFACT_SOURCE("INVALID_ARTIFACT_SOURCE"),

  /**
   * Invalid artifact server error code.
   */
  INVALID_ARTIFACT_SERVER("INVALID_ARTIFACT_SERVER", BAD_REQUEST),

  /**
   * Invalid cloud provider error code.
   */
  INVALID_CLOUD_PROVIDER("INVALID_CLOUD_PROVIDER", BAD_REQUEST);

  /**
   * The constant ARGS_NAME.
   */
  public static final String ARGS_NAME = "ARGS_NAME";
  private String code;
  private Status status = BAD_REQUEST;
  private String description;

  ErrorCode(String code) {
    this.code = code;
  }

  ErrorCode(String code, Status status) {
    this.code = code;
    this.status = status;
  }

  ErrorCode(String code, String description) {
    this.code = code;
    this.description = description;
  }

  ErrorCode(String code, Status status, String description) {
    this.code = code;
    this.status = status;
    this.description = description;
  }

  /**
   * Gets code.
   *
   * @return the code
   */
  public String getCode() {
    return code;
  }

  /**
   * Gets status.
   *
   * @return the status
   */
  public Status getStatus() {
    return status;
  }

  /**
   * Gets description.
   *
   * @return the description
   */
  public String getDescription() {
    return description != null ? description : upperUnderscoreToSpaceSepratedCamelCase(code);
  }

  /**
   * Upper underscore to space seprated camel case string.
   *
   * @param original the original
   * @return the string
   */
  public static String upperUnderscoreToSpaceSepratedCamelCase(String original) {
    return Splitter.on("_").splitToList(original).stream().map(ErrorCode::capitalize).collect(Collectors.joining(" "));
  }

  /**
   * Capitalize string.
   *
   * @param line the line
   * @return the string
   */
  public static String capitalize(final String line) {
    return line.length() > 1 ? line.charAt(0) + line.substring(1).toLowerCase() : line;
  }
}
