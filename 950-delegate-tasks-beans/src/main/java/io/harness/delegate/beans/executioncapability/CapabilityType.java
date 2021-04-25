package io.harness.delegate.beans.executioncapability;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

// ALWAYS_TRUE should not be a capability type. In this case, task validation should not even happen.
// But Validation needs to happen at delegate as its part of Handshake between Delegate and manager,
// in order for delegate to acquire a task.
// May be changed later
@TargetModule(HarnessModule._955_DELEGATE_BEANS)
public enum CapabilityType {
  SOCKET,
  ALWAYS_TRUE,
  PROCESS_EXECUTOR,
  AWS_REGION,
  SYSTEM_ENV,
  HTTP,
  HELM_INSTALL,
  CHART_MUSEUM,
  ALWAYS_FALSE,
  SMTP,
  WINRM_HOST_CONNECTION,
  SSH_HOST_CONNECTION,
  SFTP,
  PCF_AUTO_SCALAR,
  PCF_CONNECTIVITY,
  PCF_INSTALL,
  POWERSHELL,
  HELM_COMMAND,
  CLUSTER_MASTER_URL,
  SHELL_CONNECTION,
  GIT_CONNECTION,
  KUSTOMIZE,
  SMB,
  SELECTORS,
  GIT_CONNECTION_NG,
  GIT_INSTALLATION,
  LITE_ENGINE;
}
