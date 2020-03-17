package io.harness.delegate.beans.executioncapability;

// ALWAYS_TRUE should not be a capability type. In this case, task validation should not even happen.
// But Validation needs to happen at delegate as its part of Handshake between Delegate and manager,
// in order for delegate to acquire a task.
// May be changed later
public enum CapabilityType {
  SOCKET,
  ALWAYS_TRUE,
  PROCESS_EXECUTOR,
  AWS_REGION,
  SYSTEM_ENV,
  HTTP,
  HELM,
  CHART_MUSEUM,
  ALWAYS_FALSE,
  SMTP,
  WINRM_HOST_CONNECTION,
  SSH_HOST_CONNECTION,
  SFTP,
  PCF_AUTO_SCALAR,
  PCF_CONNECTIVITY
}
