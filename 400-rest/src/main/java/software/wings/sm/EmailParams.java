package software.wings.sm;

import static io.harness.expression.Expression.ALLOW_SECRETS;
import static io.harness.expression.Expression.DISALLOW_SECRETS;
import static io.harness.k8s.K8sConstants.HARNESS_KUBE_CONFIG_PATH;
import static io.harness.shell.SshSessionConfig.Builder.aSshSessionConfig;

import static java.lang.Boolean.FALSE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.ProcessExecutorCapabilityGenerator;
import io.harness.expression.Expression;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.shell.AccessType;
import io.harness.shell.AuthenticationScheme;
import io.harness.shell.KerberosConfig;
import io.harness.shell.ScriptType;
import io.harness.shell.ShellExecutorConfig;
import io.harness.shell.SshSessionConfig;

import software.wings.beans.AzureConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SSHVaultConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.core.winrm.executors.WinRmSessionConfig;
import software.wings.delegatetasks.validation.capabilities.ShellConnectionCapability;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.settings.SettingValue;
import software.wings.sm.states.ShellScriptState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.apache.commons.lang3.StringUtils;

@Value
@Builder
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class EmailParams {
  @Expression(ALLOW_SECRETS) @NonFinal @Setter String subject;
  @Expression(ALLOW_SECRETS) @NonFinal @Setter String toAddress;
  @Expression(ALLOW_SECRETS) @NonFinal @Setter String ccAddress;
  @Expression(ALLOW_SECRETS) @NonFinal @Setter String body;
}
