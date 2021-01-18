package io.harness.delegate.task.shell;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.expression.Expression;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.shell.ScriptType;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShellScriptTaskParametersNG implements TaskParameters {
  boolean executeOnDelegate;
  @Expression(ALLOW_SECRETS) String script;
  List<String> outputVars;
  String accountId;
  String executionId;
  String workingDirectory;
  Map<String, String> environmentVariables;
  ScriptType scriptType;
  K8sInfraDelegateConfig k8sInfraDelegateConfig;

  // Target Host Specific fields
  SSHKeySpecDTO sshKeySpecDTO;
  List<EncryptedDataDetail> encryptionDetails;
  String host;
}
