package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.helm.HelmDeployService;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.response.HelmCommandResponse;
import software.wings.service.impl.ContainerServiceParams;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by anubhaw on 4/10/18.
 */
public class HelmCommandValidation extends AbstractDelegateValidateTask {
  private static final Logger logger = LoggerFactory.getLogger(HelmCommandValidation.class);

  @Inject private transient HelmDeployService helmDeployService;
  @Inject private transient ContainerValidationHelper containerValidationHelper;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;

  public HelmCommandValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<DelegateConnectionResult> validate() {
    HelmCommandRequest commandRequest = (HelmCommandRequest) getParameters()[0];
    ContainerServiceParams params = commandRequest.getContainerServiceParams();
    logger.info("Running validation for task {} for cluster {}", delegateTaskId, params.getClusterName());

    boolean validated = false;
    try {
      String configLocation =
          containerDeploymentDelegateHelper.createAndGetKubeConfigLocation(commandRequest.getContainerServiceParams());
      commandRequest.setKubeConfigLocation(configLocation);
      HelmCommandResponse helmCommandResponse = helmDeployService.ensureHelmCliAndTillerInstalled(commandRequest);
      if (helmCommandResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS)) {
        validated = containerValidationHelper.validateContainerServiceParams(params);
      }
    } catch (Exception e) {
      logger.error("Helm validation failed", e);
    }

    return singletonList(DelegateConnectionResult.builder().criteria(getCriteria(params)).validated(validated).build());
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(getCriteria(((HelmCommandRequest) getParameters()[0]).getContainerServiceParams()));
  }

  private String getCriteria(ContainerServiceParams containerServiceParams) {
    return "helm: " + containerValidationHelper.getCriteria(containerServiceParams);
  }
}
