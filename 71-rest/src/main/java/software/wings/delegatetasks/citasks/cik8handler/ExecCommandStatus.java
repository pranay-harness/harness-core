package software.wings.delegatetasks.citasks.cik8handler;

public enum ExecCommandStatus {
  SUCCESS, // Command execution completed with 0 exit code.
  FAILURE, // Command execution completed with non-zero exit code.
  ERROR, // Failed to execute command due to some error.
  TIMEOUT // Timeout in command execution
}