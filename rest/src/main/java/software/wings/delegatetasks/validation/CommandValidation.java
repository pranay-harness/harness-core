package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import com.google.common.collect.Sets;

import com.jcraft.jsch.JSchException;
import org.apache.commons.lang3.StringUtils;
import software.wings.annotation.Encryptable;
import software.wings.api.DeploymentType;
import software.wings.beans.DelegateTask;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.core.ssh.executors.SshSessionFactory;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.SshHelperUtil;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;

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
    Set<String> nonSshDeploymentType = Sets.newHashSet(
        DeploymentType.AWS_CODEDEPLOY.name(), DeploymentType.ECS.name(), DeploymentType.KUBERNETES.name());
    decryptCredentials(context);
    if (!nonSshDeploymentType.contains(command.getDeploymentType())) {
      return validateHost(context.getHost().getPublicDns(), context);
    } else {
      return validateNonSshCommand(command, context);
    }
  }

  private DelegateConnectionResult validateNonSshCommand(Command command, CommandExecutionContext context) {
    try {
      // TODO(brett) - Validate non-ssh commands as well
      return DelegateConnectionResult.builder().criteria(NON_SSH_COMMAND_ALWAYS_TRUE).validated(true).build();
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    }
  }

  private DelegateConnectionResult validateHost(String hostName, CommandExecutionContext context) {
    DelegateConnectionResult.DelegateConnectionResultBuilder resultBuilder =
        DelegateConnectionResult.builder().criteria(hostName);
    try {
      SshSessionFactory.getSSHSession(SshHelperUtil.getSshSessionConfig(hostName, "HOST_CONNECTION_TEST", context))
          .disconnect();
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
    if (context.getCloudProviderSetting() != null) {
      encryptionService.decrypt(
          (Encryptable) context.getCloudProviderSetting().getValue(), context.getCloudProviderCredentials());
    }
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(getCriteria((Command) getParameters()[0], (CommandExecutionContext) getParameters()[1]));
  }

  private String getCriteria(Command command, CommandExecutionContext context) {
    Set<String> nonSshDeploymentType = Sets.newHashSet(
        DeploymentType.AWS_CODEDEPLOY.name(), DeploymentType.ECS.name(), DeploymentType.KUBERNETES.name());
    if (!nonSshDeploymentType.contains(command.getDeploymentType())) {
      return context.getHost().getPublicDns();
    } else {
      return NON_SSH_COMMAND_ALWAYS_TRUE;
    }
  }
}
