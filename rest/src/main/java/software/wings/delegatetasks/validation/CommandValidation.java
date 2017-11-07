package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;
import static software.wings.utils.HttpUtil.connectableHttpUrl;

import com.google.common.collect.Sets;

import com.jcraft.jsch.JSchException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.DeploymentType;
import software.wings.beans.DelegateTask;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.core.ssh.executors.SshSessionFactory;
import software.wings.delegatetasks.validation.DelegateConnectionResult.DelegateConnectionResultBuilder;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.SshHelperUtil;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;

/**
 * Created by brett on 11/5/17
 */
public class CommandValidation extends AbstractDelegateValidateTask {
  private static final String NON_SSH_COMMAND_ALWAYS_TRUE = "NON_SSH_COMMAND_ALWAYS_TRUE";

  private final Logger logger = LoggerFactory.getLogger(getClass());

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
      return validateHostSsh(context.getHost().getPublicDns(), context);
    } else {
      return validateNonSshConfig(context, command.getDeploymentType());
    }
  }

  private DelegateConnectionResult validateNonSshConfig(CommandExecutionContext context, String deploymentType) {
    DelegateConnectionResultBuilder resultBuilder = DelegateConnectionResult.builder();
    if (DeploymentType.KUBERNETES.name().equals(deploymentType) && context.getCloudProviderSetting() != null
        && context.getCloudProviderSetting().getValue() instanceof KubernetesConfig) {
      KubernetesConfig config = (KubernetesConfig) context.getCloudProviderSetting().getValue();
      resultBuilder.criteria(config.getMasterUrl()).validated(connectableHttpUrl(config.getMasterUrl()));
    } else if (DeploymentType.ECS.name().equals(deploymentType)) {
      resultBuilder.criteria(DeploymentType.ECS.name() + ":" + context.getRegion());
      CloseableHttpClient httpclient =
          HttpClients.custom()
              .setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(2000).setSocketTimeout(2000).build())
              .build();
      HttpUriRequest httpUriRequest =
          new HttpGet("http://169.254.169.254/latest/meta-data/placement/availability-zone");
      try {
        HttpResponse httpResponse = httpclient.execute(httpUriRequest);
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        logger.info("Status code checking region: " + statusCode);
        HttpEntity entity = httpResponse.getEntity();
        String content =
            entity != null ? EntityUtils.toString(entity, ContentType.getOrDefault(entity).getCharset()) : "";
        logger.info("Delegate AWS region: " + content);
        resultBuilder.validated(StringUtils.startsWith(content, context.getRegion()));
      } catch (IOException e) {
        resultBuilder.validated(false);
      }
    } else {
      resultBuilder.criteria(NON_SSH_COMMAND_ALWAYS_TRUE).validated(true);
    }
    return resultBuilder.build();
  }

  private DelegateConnectionResult validateHostSsh(String hostName, CommandExecutionContext context) {
    DelegateConnectionResultBuilder resultBuilder = DelegateConnectionResult.builder().criteria(hostName);
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
    } else if (DeploymentType.KUBERNETES.name().equals(command.getDeploymentType())
        && context.getCloudProviderSetting() != null
        && context.getCloudProviderSetting().getValue() instanceof KubernetesConfig) {
      return ((KubernetesConfig) context.getCloudProviderSetting().getValue()).getMasterUrl();
    } else if (DeploymentType.ECS.name().equals(command.getDeploymentType())) {
      return DeploymentType.ECS.name() + ":" + context.getRegion();
    } else {
      return NON_SSH_COMMAND_ALWAYS_TRUE;
    }
  }
}
