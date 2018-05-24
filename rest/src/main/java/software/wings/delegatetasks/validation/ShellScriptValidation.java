package software.wings.delegatetasks.validation;

import static io.harness.govern.Switch.unhandled;
import static java.util.Collections.singletonList;
import static software.wings.beans.ErrorCode.INVALID_CREDENTIAL;
import static software.wings.beans.ErrorCode.SSL_HANDSHAKE_FAILED;
import static software.wings.common.Constants.HARNESS_KUBE_CONFIG_PATH;
import static software.wings.core.ssh.executors.SshSessionFactory.getSSHSession;
import static software.wings.utils.WinRmHelperUtil.GetErrorDetailsFromWinRmClientException;

import com.google.inject.Inject;

import com.jcraft.jsch.JSchException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AzureConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.ResponseMessage;
import software.wings.beans.SettingAttribute;
import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.core.winrm.executors.WinRmSession;
import software.wings.core.winrm.executors.WinRmSessionConfig;
import software.wings.delegatetasks.validation.DelegateConnectionResult.DelegateConnectionResultBuilder;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.settings.SettingValue;

import java.util.List;
import java.util.function.Consumer;

public class ShellScriptValidation extends AbstractDelegateValidateTask {
  private static final Logger logger = LoggerFactory.getLogger(ShellScriptValidation.class);

  @Inject private transient EncryptionService encryptionService;
  @Inject private transient ContainerValidationHelper containerValidationHelper;

  public ShellScriptValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    Object[] parameters = getParameters();
    return singletonList(validate((ShellScriptParameters) parameters[0]));
  }

  private DelegateConnectionResult validate(ShellScriptParameters parameters) {
    DelegateConnectionResultBuilder resultBuilder = DelegateConnectionResult.builder().criteria(getCriteria().get(0));

    boolean validated = true;
    if (parameters.isExecuteOnDelegate()) {
      ContainerServiceParams containerServiceParams = parameters.getContainerServiceParams();
      if (containerServiceParams != null) {
        SettingAttribute settingAttribute = containerServiceParams.getSettingAttribute();
        if (settingAttribute != null) {
          SettingValue value = settingAttribute.getValue();
          boolean useKubernetesDelegate =
              value instanceof KubernetesClusterConfig && ((KubernetesClusterConfig) value).isUseKubernetesDelegate();
          boolean isKubernetes = value instanceof KubernetesConfig || value instanceof GcpConfig
              || value instanceof AzureConfig || value instanceof KubernetesClusterConfig;
          if (useKubernetesDelegate || (isKubernetes && parameters.getScript().contains(HARNESS_KUBE_CONFIG_PATH))) {
            validated = containerValidationHelper.validateContainerServiceParams(containerServiceParams);
          }
        }
      }
      return resultBuilder.validated(validated).build();
    }

    switch (parameters.getConnectionType()) {
      case SSH:
        try {
          SshSessionConfig expectedSshConfig = parameters.sshSessionConfig(encryptionService);
          getSSHSession(expectedSshConfig).disconnect();

          resultBuilder.validated(true);
        } catch (JSchException jschEx) {
          // Invalid credentials error is still a valid connection
          resultBuilder.validated(StringUtils.contains(jschEx.getMessage(), "Auth"));
        } catch (Exception ex) {
          resultBuilder.validated(false);
        }
        break;

      case WINRM:
        try {
          WinRmSessionConfig winrmConfig = parameters.winrmSessionConfig(encryptionService);
          try (WinRmSession ignore = new WinRmSession(winrmConfig)) {
            resultBuilder.validated(true);
          }
        } catch (Exception e) {
          ResponseMessage details = GetErrorDetailsFromWinRmClientException(e);
          logger.error(details.getMessage(), e);
          resultBuilder.validated(details.getCode() == SSL_HANDSHAKE_FAILED || details.getCode() == INVALID_CREDENTIAL);
        }
        break;

      default:
        unhandled(parameters.getConnectionType());
        resultBuilder.validated(false);
    }

    return resultBuilder.build();
  }

  @Override
  public List<String> getCriteria() {
    ShellScriptParameters parameters = (ShellScriptParameters) getParameters()[0];

    String criteria;
    if (parameters.isExecuteOnDelegate()) {
      criteria = "localhost";
      ContainerServiceParams containerServiceParams = parameters.getContainerServiceParams();
      if (containerServiceParams != null) {
        SettingAttribute settingAttribute = containerServiceParams.getSettingAttribute();
        if (settingAttribute != null) {
          SettingValue value = settingAttribute.getValue();
          boolean useKubernetesDelegate =
              value instanceof KubernetesClusterConfig && ((KubernetesClusterConfig) value).isUseKubernetesDelegate();
          boolean isKubernetes = value instanceof KubernetesConfig || value instanceof GcpConfig
              || value instanceof AzureConfig || value instanceof KubernetesClusterConfig;
          if (useKubernetesDelegate || (isKubernetes && parameters.getScript().contains(HARNESS_KUBE_CONFIG_PATH))) {
            criteria = containerValidationHelper.getCriteria(containerServiceParams);
          }
        }
      }
    } else {
      criteria = parameters.getHost();
    }
    return singletonList(criteria);
  }
}
