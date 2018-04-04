package software.wings.delegatetasks.validation;

import static io.harness.govern.Switch.unhandled;
import static io.harness.network.Http.connectableHttpUrl;
import static java.util.Collections.singletonList;
import static software.wings.core.ssh.executors.SshSessionFactory.getSSHSession;
import static software.wings.utils.SshHelperUtil.getSshSessionConfig;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.jcraft.jsch.JSchException;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
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
import software.wings.delegatetasks.validation.DelegateConnectionResult.DelegateConnectionResultBuilder;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.settings.SettingValue;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by brett on 11/5/17
 */
public class CommandValidation extends AbstractDelegateValidateTask {
  private static final String NON_SSH_COMMAND_ALWAYS_TRUE = "NON_SSH_COMMAND_ALWAYS_TRUE";
  private static final Set<String> NON_SSH_DEPLOYMENT_TYPES = Sets.newHashSet(
      DeploymentType.AWS_CODEDEPLOY.name(), DeploymentType.ECS.name(), DeploymentType.KUBERNETES.name());

  @Inject @Transient private transient EncryptionService encryptionService;
  @Inject @Transient private transient GkeClusterService gkeClusterService;
  @Inject @Transient private transient AzureHelperService azureHelperService;

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
    if (NON_SSH_DEPLOYMENT_TYPES.contains(context.getDeploymentType())) {
      return validateNonSsh(context);
    } else {
      return validateHostSsh(context.getHost().getPublicDns(), context);
    }
  }

  private DelegateConnectionResult validateNonSsh(CommandExecutionContext context) {
    String criteria = getCriteria(context);
    String deploymentType = context.getDeploymentType().toString();
    DelegateConnectionResultBuilder resultBuilder = DelegateConnectionResult.builder().criteria(criteria);
    if (DeploymentType.KUBERNETES.name().equals(deploymentType) && context.getCloudProviderSetting() != null) {
      resultBuilder.validated(connectableHttpUrl(getKubernetesMasterUrl(context)));
    } else if (DeploymentType.ECS.name().equals(deploymentType)
        || DeploymentType.AWS_CODEDEPLOY.name().equals(deploymentType)) {
      String region = null;
      if (context.getContainerSetupParams() != null) {
        region = ((EcsSetupParams) context.getContainerSetupParams()).getRegion();
      } else if (context.getContainerResizeParams() != null) {
        region = ((EcsResizeParams) context.getContainerResizeParams()).getRegion();
      } else if (context.getCodeDeployParams() != null) {
        region = context.getCodeDeployParams().getRegion();
      }
      resultBuilder.validated(region == null || AwsHelperService.isInAwsRegion(region));
    } else {
      resultBuilder.validated(true);
    }
    return resultBuilder.build();
  }

  private DelegateConnectionResult validateHostSsh(String hostName, CommandExecutionContext context) {
    String criteria = getCriteria(context);
    DelegateConnectionResultBuilder resultBuilder = DelegateConnectionResult.builder().criteria(criteria);
    try {
      getSSHSession(getSshSessionConfig(hostName, "HOST_CONNECTION_TEST", context, 20)).disconnect();
      resultBuilder.validated(true);
    } catch (JSchException jschEx) {
      // Invalid credentials error is still a valid connection
      resultBuilder.validated(StringUtils.contains(jschEx.getMessage(), "Auth"));
    }
    return resultBuilder.build();
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
    String deploymentType = context.getDeploymentType().toString();
    Set<String> nonSshDeploymentType = Sets.newHashSet(
        DeploymentType.AWS_CODEDEPLOY.name(), DeploymentType.ECS.name(), DeploymentType.KUBERNETES.name());
    if (!nonSshDeploymentType.contains(deploymentType)) {
      return context.getHost().getPublicDns();
    } else if (DeploymentType.KUBERNETES.name().equals(deploymentType) && context.getCloudProviderSetting() != null) {
      return getKubernetesCriteria(context);
    } else if (DeploymentType.ECS.name().equals(deploymentType)
        || DeploymentType.AWS_CODEDEPLOY.name().equals(deploymentType)) {
      String region = null;
      if (context.getContainerSetupParams() != null) {
        region = ((EcsSetupParams) context.getContainerSetupParams()).getRegion();
      } else if (context.getContainerResizeParams() != null) {
        region = ((EcsResizeParams) context.getContainerResizeParams()).getRegion();
      } else if (context.getCodeDeployParams() != null) {
        region = context.getCodeDeployParams().getRegion();
      }
      return region == null ? NON_SSH_COMMAND_ALWAYS_TRUE : "AWS:" + region;
    } else {
      unhandled(deploymentType);
    }
    return NON_SSH_COMMAND_ALWAYS_TRUE;
  }

  private String getKubernetesCriteria(CommandExecutionContext context) {
    SettingValue value = context.getCloudProviderSetting().getValue();
    if (value instanceof KubernetesConfig) {
      return ((KubernetesConfig) value).getMasterUrl();
    } else {
      String clusterName = null;
      if (context.getContainerSetupParams() != null) {
        KubernetesSetupParams setupParams = (KubernetesSetupParams) context.getContainerSetupParams();
        clusterName = setupParams.getClusterName();
      } else if (context.getContainerResizeParams() != null) {
        KubernetesResizeParams resizeParams = (KubernetesResizeParams) context.getContainerResizeParams();
        clusterName = resizeParams.getClusterName();
      }
      return context.getCloudProviderSetting().getName() + "|" + clusterName;
    }
  }
}
