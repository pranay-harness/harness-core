package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.helpers.ext.helm.HelmDeployService;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.response.HelmCommandResponse;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.settings.SettingValue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Created by anubhaw on 4/10/18.
 */
public class HelmCommandValidation extends AbstractDelegateValidateTask {
  private static final Logger logger = LoggerFactory.getLogger(HelmCommandValidation.class);
  @Inject @Transient private transient HelmDeployService helmDeployService;

  public HelmCommandValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<DelegateConnectionResult> validate() {
    HelmCommandRequest commandRequest = (HelmCommandRequest) getParameters()[0];
    logger.info("Running validation for task {} for cluster {}", delegateTaskId,
        commandRequest.getContainerServiceParams().getClusterName());

    boolean validated = false;
    try {
      HelmCommandResponse helmCommandResponse = helmDeployService.ensureHelmCliAndTillerInstalled(commandRequest);
      validated = helmCommandResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS);
    } catch (InterruptedException | IOException | TimeoutException e) {
      logger.error("Helm validation failed", e);
    }

    return singletonList(DelegateConnectionResult.builder()
                             .criteria(getCriteria(commandRequest.getContainerServiceParams()))
                             .validated(validated)
                             .build());
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(getCriteria((ContainerServiceParams) getParameters()[0]));
  }

  private String getCriteria(ContainerServiceParams containerServiceParams) {
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();
    if (value instanceof AwsConfig) {
      return "AWS:" + containerServiceParams.getRegion();
    } else if (value instanceof KubernetesConfig) {
      return ((KubernetesConfig) value).getMasterUrl();
    } else {
      return containerServiceParams.getSettingAttribute().getName() + "|" + containerServiceParams.getClusterName();
    }
  }
}
