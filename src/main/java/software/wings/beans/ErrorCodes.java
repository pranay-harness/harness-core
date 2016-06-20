package software.wings.beans;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import javax.ws.rs.core.Response.Status;

/**
 * The enum Error codes.
 */
public enum ErrorCodes {
  /**
   * Default error code error codes.
   */
  DEFAULT_ERROR_CODE("DEFAULT_ERROR_CODE"), /**
                                             * Invalid argument error codes.
                                             */
  INVALID_ARGUMENT("INVALID_ARGUMENT"),

  /**
   * User already registered error codes.
   */
  USER_ALREADY_REGISTERED("USER_ALREADY_REGISTERED", CONFLICT),

  /**
   * User does not exist error codes.
   */
  USER_DOES_NOT_EXIST("USER_DOES_NOT_EXIST", UNAUTHORIZED),
  /**
   * Email not verified error codes.
   */
  EMAIL_NOT_VERIFIED("EMAIL_NOT_VERIFIED", UNAUTHORIZED),

  /**
   * Email verification token not found error codes.
   */
  EMAIL_VERIFICATION_TOKEN_NOT_FOUND("EMAIL_VERIFICATION_TOKEN_NOT_FOUND"),
  /**
   * Invalid token error codes.
   */
  INVALID_TOKEN("INVALID_TOKEN", UNAUTHORIZED), /**
                                                 * Expired token error codes.
                                                 */
  EXPIRED_TOKEN("EXPIRED_TOKEN", UNAUTHORIZED), /**
                                                 * Access denied error codes.
                                                 */
  ACCESS_DENIED("ACCESS_DENIED", FORBIDDEN), /**
                                              * Invalid credential error codes.
                                              */
  INVALID_CREDENTIAL("INVALID_CREDENTIAL_ERROR", UNAUTHORIZED), /**
                                                                 * Invalid key error codes.
                                                                 */
  INVALID_KEY("INVALID_KEY_ERROR"), /**
                                     * Invalid keypath error codes.
                                     */
  INVALID_KEYPATH("INVALID_KEYPATH_ERROR"), /**
                                             * Unknown host error codes.
                                             */
  UNKNOWN_HOST("UNKNOWN_HOST_ERROR"), /**
                                       * Unreachable host error codes.
                                       */
  UNREACHABLE_HOST("UNREACHABLE_HOST_ERROR"), /**
                                               * Invalid port error codes.
                                               */
  INVALID_PORT("INVALID_OR_BLOCKED_PORT_ERROR"), /**
                                                  * Ssh session timeout error codes.
                                                  */
  SSH_SESSION_TIMEOUT("SSH_SESSION_TIMEOUT"), /**
                                               * Socket connection error error codes.
                                               */
  SOCKET_CONNECTION_ERROR("SSH_SOCKET_CONNECTION_ERROR"), /**
                                                           * Socket connection timeout error codes.
                                                           */
  SOCKET_CONNECTION_TIMEOUT("SOCKET_CONNECTION_TIMEOUT_ERROR"), /**
                                                                 * Unknown error error codes.
                                                                 */
  UNKNOWN_ERROR("UNKNOWN_ERROR"), /**
                                   * Unknown executor type error error codes.
                                   */
  UNKNOWN_EXECUTOR_TYPE_ERROR("UNKNOWN_EXECUTOR_TYPE_ERROR"),

  /**
   * Duplicate state names error codes.
   */
  DUPLICATE_STATE_NAMES("DUPLICATE_STATE_NAMES"), /**
                                                   * Transition not linked error codes.
                                                   */
  TRANSITION_NOT_LINKED("TRANSITION_NOT_LINKED"), /**
                                                   * Transition to incorrect state error codes.
                                                   */
  TRANSITION_TO_INCORRECT_STATE("TRANSITION_TO_INCORRECT_STATE"), /**
                                                                   * Transition type null error codes.
                                                                   */
  TRANSITION_TYPE_NULL("TRANSITION_TYPE_NULL"), /**
                                                 * States with dup transitions error codes.
                                                 */
  STATES_WITH_DUP_TRANSITIONS("STATES_WITH_DUP_TRANSITIONS"), /**
                                                               * Non fork states error codes.
                                                               */
  NON_FORK_STATES("NON_FORK_STATES"), /**
                                       * Non repeat states error codes.
                                       */
  NON_REPEAT_STATES("NON_REPEAT_STATES"), /**
                                           * Initial state not defined error codes.
                                           */
  INITIAL_STATE_NOT_DEFINED("INITIAL_STATE_NOT_DEFINED"), /**
                                                           * File integrity check failed error codes.
                                                           */
  FILE_INTEGRITY_CHECK_FAILED("FILE_INTEGRITY_CHECK_FAILED"), /**
                                                               * Invalid url error codes.
                                                               */
  INVALID_URL("INVALID_URL"), /**
                               * File download failed error codes.
                               */
  FILE_DOWNLOAD_FAILED("FILE_DOWNLOAD_FAILED"), /**
                                                 * Platform software delete error error codes.
                                                 */
  PLATFORM_SOFTWARE_DELETE_ERROR("PLATFORM_SOFTWARE_DELETE_ERROR"), /**
                                                                     * Invalid csv file error codes.
                                                                     */
  INVALID_CSV_FILE("INVALID_CSV_FILE"), /**
                                         * Invalid request error codes.
                                         */
  INVALID_REQUEST("INVALID_REQUEST"), /**
                                       * Pipeline already triggered error codes.
                                       */
  PIPELINE_ALREADY_TRIGGERED("PIPELINE_ALREADY_TRIGGERED"), /**
                                                             * Non existing pipeline error codes.
                                                             */
  NON_EXISTING_PIPELINE("NON_EXISTING_PIPELINE"),

  /**
   * Duplicate command names error codes.
   */
  DUPLICATE_COMMAND_NAMES("DUPLICATE_COMMAND_NAMES"), /**
                                                       * Invalid pipeline error codes.
                                                       */
  INVALID_PIPELINE("INVALID_PIPELINE"), /**
                                         * Command does not exist error codes.
                                         */
  COMMAND_DOES_NOT_EXIST("COMMAND_DOES_NOT_EXIST"),

  /**
   * Duplicate artifactsource names error codes.
   */
  DUPLICATE_ARTIFACTSOURCE_NAMES("DUPLICATE_ARTIFACTSOURCE_NAMES");

  /**
   * The constant ARGS_NAME.
   */
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
}
