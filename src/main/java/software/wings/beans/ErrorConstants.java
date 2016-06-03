package software.wings.beans;

/**
 * The Interface ErrorConstants.
 */
public interface ErrorConstants {
  String DEFAULT_ERROR_CODE = "DEFAULT_ERROR_CODE";
  String INVALID_APP_NAME = "INVALID_APP_NAME";
  String INVALID_ARGUMENT = "INVALID_ARGUMENT";
  String INVALID_TOKEN = "INVALID_TOKEN";
  String EXPIRED_TOKEN = "EXPIRED_TOKEN";
  String ACCESS_DENIED = "ACCESS_DENIED";

  String INVALID_CREDENTIAL = "INVALID_CREDENTIAL_ERROR";
  String INVALID_KEY = "INVALID_KEY_ERROR";
  String INVALID_KEYPATH = "INVALID_KEYPATH_ERROR";
  String UNKNOWN_HOST = "UNKNOWN_HOST_ERROR";
  String UNREACHABLE_HOST = "UNREACHABLE_HOST_ERROR";
  String INVALID_PORT = "INVALID_OR_BLOCKED_PORT_ERROR";
  String COMMAND_EXEC_TIMEOUT = "COMMAND_EXEC_TIMEOUT";
  String SSH_SESSION_TIMEOUT = "SSH_SESSION_TIMEOUT";
  String SOCKET_CONNECTION_ERROR = "SSH_SOCKET_CONNECTION_ERROR";
  String SOCKET_CONNECTION_TIMEOUT = "SOCKET_CONNECTION_TIMEOUT_ERROR";
  String UNKNOWN_ERROR = "UNKNOWN_ERROR";
  String UNKNOWN_COMMAND_UNIT_ERROR = "UNKNOWN_COMMAND_UNIT_ERROR";
  String UNKNOWN_EXECUTOR_TYPE_ERROR = "UNKNOWN_EXECUTOR_TYPE_ERROR";
  String NOT_INITIALIZED = "NOT_INITIALIZED";
  String ALREADY_INITIALIZED = "ALREADY_INITIALIZED";

  String DUPLICATE_STATE_NAMES = "DUPLICATE_STATE_NAMES";
  String TRANSITION_NOT_LINKED = "TRANSITION_NOT_LINKED";
  String TRANSITION_TO_INCORRECT_STATE = "TRANSITION_TO_INCORRECT_STATE";
  String TRANSITION_TYPE_NULL = "TRANSITION_TYPE_NULL";
  String STATES_WITH_DUP_TRANSITIONS = "STATES_WITH_DUP_TRANSITIONS";
  String NON_FORK_STATES = "NON_FORK_STATES";
  String NON_REPEAT_STATES = "NON_REPEAT_STATES";
  String INITIAL_STATE_NOT_DEFINED = "INITIAL_STATE_NOT_DEFINED";
  String FILE_INTEGRITY_CHECK_FAILED = "FILE_INTEGRITY_CHECK_FAILED";
  String INVALID_URL = "INVALID_URL";
  String FILE_DOWNLOAD_FAILED = "FILE_DOWNLOAD_FAILED";
  String PLATFORM_SOFTWARE_DELETE_ERROR = "PLATFORM_SOFTWARE_DELETE_ERROR";
  String INVALID_CSV_FILE = "INVALID_CSV_FILE";
  String UNKNOWN_STENCIL_TYPE = "UNKNOWN_STENCIL_TYPE";
  String ARGS_NAME = "ARGS_NAME";
  String INVALID_REQUEST = "INVALID_REQUEST";
  String PIPELINE_ALREADY_TRIGGERED = "PIPELINE_ALREADY_TRIGGERED";
  String NON_EXISTING_PIPELINE = "NON_EXISTING_PIPELINE";

  String DUPLICATE_COMMAND_NAMES = "DUPLICATE_COMMAND_NAMES";
  String DUPLICATE_ARTIFACTSOURCE_NAMES = "DUPLICATE_ARTIFACTSOURCE_NAMES";
}
