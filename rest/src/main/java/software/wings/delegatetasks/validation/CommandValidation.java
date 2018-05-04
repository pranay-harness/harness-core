package software.wings.delegatetasks.validation;

import static io.harness.govern.Switch.unhandled;
import static io.harness.network.Http.connectableHttpUrl;
import static java.util.Collections.singletonList;
import static software.wings.core.ssh.executors.SshSessionFactory.getSSHSession;
import static software.wings.utils.SshHelperUtil.getSshSessionConfig;
import static software.wings.utils.WinRmHelperUtil.HandleWinRmClientException;
import static software.wings.utils.message.MessageConstants.DELEGATE_DATA;
import static software.wings.utils.message.MessageConstants.DELEGATE_NAME;

import com.google.inject.Inject;

import com.jcraft.jsch.JSchException;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.DeploymentType;
import software.wings.beans.AzureConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.EcsResizeParams;
import software.wings.beans.command.EcsSetupParams;
import software.wings.beans.command.KubernetesResizeParams;
import software.wings.beans.command.KubernetesSetupParams;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.core.winrm.executors.WinRmSession;
import software.wings.core.winrm.executors.WinRmSessionConfig;
import software.wings.delegatetasks.validation.DelegateConnectionResult.DelegateConnectionResultBuilder;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.settings.SettingValue;
import software.wings.utils.message.MessageService;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Created by brett on 11/5/17
 */
public class CommandValidation extends AbstractDelegateValidateTask {
  private static final String ALWAYS_TRUE = "ALWAYS_TRUE";
  @Transient private static final Logger logger = LoggerFactory.getLogger(CommandValidation.class);

  @Inject private transient EncryptionService encryptionService;
  @Inject private transient GkeClusterService gkeClusterService;
  @Inject private transient AzureHelperService azureHelperService;
  @Inject private transient MessageService messageService;

  public CommandValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    Object[] parameters = getParameters();
    return singletonList(validate((CommandExecutionContext) parameters[1]));
  }

  private DelegateConnectionResult validate(CommandExecutionContext context) {
    decryptCredentials(context);
    DeploymentType deploymentType = DeploymentType.valueOf(context.getDeploymentType());
    logger.info("Processing validate for deploymentType %s", deploymentType.name());
    switch (deploymentType) {
      case KUBERNETES:
        return validateKubernetes(context);
      case ECS:
        return validateEcs(context);
      case AWS_CODEDEPLOY:
        return validateAwsCodeDelpoy(context);
      case WINRM:
        return validateHostWinRm(context);
      case SSH:
        return validateHostSsh(context);
      case AMI:
      case AWS_LAMBDA:
        return validateAlwaysTrue();
      default:
        unhandled(deploymentType);
        throw new WingsException(ErrorCode.INVALID_ARGUMENT)
            .addParam("args", "deploymentType is not handled: " + deploymentType.name());
    }
  }

  private DelegateConnectionResult validateHostSsh(CommandExecutionContext context) {
    String hostName = context.getHost().getPublicDns();
    DelegateConnectionResultBuilder resultBuilder = DelegateConnectionResult.builder().criteria(getCriteria(context));
    try {
      getSSHSession(getSshSessionConfig(hostName, "HOST_CONNECTION_TEST", context, 20)).disconnect();
      resultBuilder.validated(true);
    } catch (JSchException jschEx) {
      // Invalid credentials error is still a valid connection
      resultBuilder.validated(StringUtils.contains(jschEx.getMessage(), "Auth"));
    }
    return resultBuilder.build();
  }

  private DelegateConnectionResult validateHostWinRm(CommandExecutionContext context) {
    DelegateConnectionResultBuilder resultBuilder = DelegateConnectionResult.builder().criteria(getCriteria(context));
    WinRmSessionConfig config = context.winrmSessionConfig("HOST_CONNECTION_TEST");
    try (WinRmSession session = new WinRmSession(config)) {
      resultBuilder.validated(true);
    } catch (Exception e) {
      String errorMessage = HandleWinRmClientException(e);
      resultBuilder.validated(!StringUtils.contains(errorMessage, "Cannot reach remote host"));
    }
    return resultBuilder.build();
  }

  private DelegateConnectionResult validateKubernetes(CommandExecutionContext context) {
    SettingValue config = context.getCloudProviderSetting().getValue();
    boolean validated;
    if (config instanceof KubernetesClusterConfig && ((KubernetesClusterConfig) config).isUseKubernetesDelegate()) {
      String delegateName = messageService.getData(DELEGATE_DATA, DELEGATE_NAME, String.class);
      validated = Objects.equals(delegateName, ((KubernetesClusterConfig) config).getDelegateName());
    } else {
      validated = connectableHttpUrl(getKubernetesMasterUrl(context));
    }
    return DelegateConnectionResult.builder().criteria(getCriteria(context)).validated(validated).build();
  }

  private DelegateConnectionResult validateEcs(CommandExecutionContext context) {
    String region = null;
    if (context.getContainerSetupParams() != null) {
      region = ((EcsSetupParams) context.getContainerSetupParams()).getRegion();
    } else if (context.getContainerResizeParams() != null) {
      region = ((EcsResizeParams) context.getContainerResizeParams()).getRegion();
    }
    return DelegateConnectionResult.builder()
        .criteria(getCriteria(context))
        .validated(region == null || AwsHelperService.isInAwsRegion(region))
        .build();
  }

  private DelegateConnectionResult validateAwsCodeDelpoy(CommandExecutionContext context) {
    String region = null;
    if (context.getCodeDeployParams() != null) {
      region = context.getCodeDeployParams().getRegion();
    }
    return DelegateConnectionResult.builder()
        .criteria(getCriteria(context))
        .validated(region == null || AwsHelperService.isInAwsRegion(region))
        .build();
  }

  private DelegateConnectionResult validateAlwaysTrue() {
    return DelegateConnectionResult.builder().criteria(ALWAYS_TRUE).validated(true).build();
  }

  private void decryptCredentials(CommandExecutionContext context) {
    if (context.getHostConnectionAttributes() != null) {
      encryptionService.decrypt(
          (Encryptable) context.getHostConnectionAttributes().getValue(), context.getHostConnectionCredentials());
    }
    if (context.getBastionConnectionAttributes() != null) {
      encryptionService.decrypt(
          (Encryptable) context.getBastionConnectionAttributes().getValue(), context.getBastionConnectionCredentials());
    }
    if (context.getWinrmConnectionAttributes() != null) {
      encryptionService.decrypt(
          context.getWinrmConnectionAttributes(), context.getWinrmConnectionEncryptedDataDetails());
    }
  }

  private String getKubernetesMasterUrl(CommandExecutionContext context) {
    KubernetesConfig kubernetesConfig;
    SettingAttribute settingAttribute = context.getCloudProviderSetting();
    SettingValue value = settingAttribute.getValue();
    if (value instanceof KubernetesConfig) {
      kubernetesConfig = (KubernetesConfig) value;
    } else {
      String clusterName = null;
      String namespace = null;
      String subscriptionId = null;
      String resourceGroup = null;
      if (context.getContainerSetupParams() != null) {
        KubernetesSetupParams setupParams = (KubernetesSetupParams) context.getContainerSetupParams();
        clusterName = setupParams.getClusterName();
        namespace = setupParams.getNamespace();
        subscriptionId = setupParams.getSubscriptionId();
        resourceGroup = setupParams.getResourceGroup();
      } else if (context.getContainerResizeParams() != null) {
        KubernetesResizeParams resizeParams = (KubernetesResizeParams) context.getContainerResizeParams();
        clusterName = resizeParams.getClusterName();
        namespace = resizeParams.getNamespace();
        subscriptionId = resizeParams.getSubscriptionId();
        resourceGroup = resizeParams.getResourceGroup();
      }
      List<EncryptedDataDetail> edd = context.getCloudProviderCredentials();
      if (value instanceof GcpConfig) {
        kubernetesConfig = gkeClusterService.getCluster(settingAttribute, edd, clusterName, namespace);
      } else if (value instanceof AzureConfig) {
        AzureConfig azureConfig = (AzureConfig) value;
        kubernetesConfig = azureHelperService.getKubernetesClusterConfig(
            azureConfig, edd, subscriptionId, resourceGroup, clusterName, namespace);
      } else if (value instanceof KubernetesClusterConfig) {
        kubernetesConfig = ((KubernetesClusterConfig) value).createKubernetesConfig(namespace);
      } else {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT)
            .addParam("args", "Unknown kubernetes cloud provider setting value: " + value.getType());
      }
    }
    return kubernetesConfig.getMasterUrl();
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(getCriteria((CommandExecutionContext) getParameters()[1]));
  }

  private String getCriteria(CommandExecutionContext context) {
    String region = null;
    DeploymentType deploymentType = DeploymentType.valueOf(context.getDeploymentType());
    switch (deploymentType) {
      case KUBERNETES:
        SettingValue config = context.getCloudProviderSetting().getValue();
        return config instanceof KubernetesClusterConfig && ((KubernetesClusterConfig) config).isUseKubernetesDelegate()
            ? "delegate-name: " + ((KubernetesClusterConfig) config).getDelegateName()
            : getKubernetesMasterUrl(context);
      case ECS:
        if (context.getContainerSetupParams() != null) {
          region = ((EcsSetupParams) context.getContainerSetupParams()).getRegion();
        } else if (context.getContainerResizeParams() != null) {
          region = ((EcsResizeParams) context.getContainerResizeParams()).getRegion();
        }
        return getAwsRegionCriteria(region);
      case AWS_CODEDEPLOY:
        if (context.getCodeDeployParams() != null) {
          region = context.getCodeDeployParams().getRegion();
        }
        return getAwsRegionCriteria(region);
      case WINRM:
      case SSH:
        return context.getHost().getPublicDns();
      case AMI:
      case AWS_LAMBDA:
        return ALWAYS_TRUE;
      default:
        unhandled(deploymentType);
        throw new WingsException(ErrorCode.INVALID_ARGUMENT)
            .addParam("args", "deploymentType is not handled: " + deploymentType.name());
    }
  }

  private String getAwsRegionCriteria(String region) {
    return region == null ? ALWAYS_TRUE : "AWS: " + region;
  }
}
