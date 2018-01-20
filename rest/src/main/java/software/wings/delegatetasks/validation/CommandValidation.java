package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;
import static software.wings.core.ssh.executors.SshSessionFactory.getSSHSession;
import static software.wings.utils.HttpUtil.connectableHttpUrl;
import static software.wings.utils.SshHelperUtil.getSshSessionConfig;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.jcraft.jsch.JSchException;
import org.apache.commons.lang3.StringUtils;
import software.wings.annotation.Encryptable;
import software.wings.api.DeploymentType;
import software.wings.beans.DelegateTask;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.EcsResizeParams;
import software.wings.beans.command.EcsSetupParams;
import software.wings.delegatetasks.validation.DelegateConnectionResult.DelegateConnectionResultBuilder;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by brett on 11/5/17
 */
public class CommandValidation extends AbstractDelegateValidateTask {
  private static final String NON_SSH_COMMAND_ALWAYS_TRUE = "NON_SSH_COMMAND_ALWAYS_TRUE";

  @Inject private EncryptionService encryptionService;

  public CommandValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    Object[] parameters = getParameters();
    return singletonList(validate((Command) parameters[0], (CommandExecutionContext) parameters[1]));
  }

  private DelegateConnectionResult validate(Command command, CommandExecutionContext context) {
    DelegateConnectionResultBuilder resultBuilder =
        DelegateConnectionResult.builder().criteria(getCriteria(command.getDeploymentType(), context));
    Set<String> nonSshDeploymentType = Sets.newHashSet(
        DeploymentType.AWS_CODEDEPLOY.name(), DeploymentType.ECS.name(), DeploymentType.KUBERNETES.name());
    decryptCredentials(context);
    if (nonSshDeploymentType.contains(command.getDeploymentType())) {
      validateNonSsh(resultBuilder, context, command.getDeploymentType());
    } else {
      validateHostSsh(resultBuilder, context.getHost().getPublicDns(), context);
    }
    return resultBuilder.build();
  }

  private void validateNonSsh(
      DelegateConnectionResultBuilder resultBuilder, CommandExecutionContext context, String deploymentType) {
    if (DeploymentType.KUBERNETES.name().equals(deploymentType) && context.getCloudProviderSetting() != null
        && context.getCloudProviderSetting().getValue() instanceof KubernetesConfig) {
      resultBuilder.validated(
          connectableHttpUrl(((KubernetesConfig) context.getCloudProviderSetting().getValue()).getMasterUrl()));
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
  }

  private void validateHostSsh(
      DelegateConnectionResultBuilder resultBuilder, String hostName, CommandExecutionContext context) {
    try {
      getSSHSession(getSshSessionConfig(hostName, "HOST_CONNECTION_TEST", context, 20)).disconnect();
      resultBuilder.validated(true);
    } catch (JSchException jschEx) {
      // Invalid credentials error is still a valid connection
      resultBuilder.validated(StringUtils.contains(jschEx.getMessage(), "Auth"));
    }
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
    if (context.getCloudProviderSetting() != null) {
      encryptionService.decrypt(
          (Encryptable) context.getCloudProviderSetting().getValue(), context.getCloudProviderCredentials());
    }
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(
        getCriteria(((Command) getParameters()[0]).getDeploymentType(), (CommandExecutionContext) getParameters()[1]));
  }

  private String getCriteria(String deploymentType, CommandExecutionContext context) {
    Set<String> nonSshDeploymentType = Sets.newHashSet(
        DeploymentType.AWS_CODEDEPLOY.name(), DeploymentType.ECS.name(), DeploymentType.KUBERNETES.name());
    if (!nonSshDeploymentType.contains(deploymentType)) {
      return context.getHost().getPublicDns();
    } else if (DeploymentType.KUBERNETES.name().equals(deploymentType) && context.getCloudProviderSetting() != null
        && context.getCloudProviderSetting().getValue() instanceof KubernetesConfig) {
      return ((KubernetesConfig) context.getCloudProviderSetting().getValue()).getMasterUrl();
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
      return NON_SSH_COMMAND_ALWAYS_TRUE;
    }
  }
}
