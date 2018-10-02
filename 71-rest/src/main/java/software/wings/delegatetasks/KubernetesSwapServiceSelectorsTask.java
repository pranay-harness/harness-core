package software.wings.delegatetasks;

import static java.util.Collections.emptyList;
import static software.wings.service.impl.KubernetesHelperService.toDisplayYaml;
import static software.wings.sm.states.KubernetesSwapServiceSelectors.KUBERNETES_SWAP_SERVICE_SELECTORS_COMMAND_NAME;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.Service;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.container.KubernetesSwapServiceSelectorsParams;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.states.KubernetesSwapServiceSelectorsResponse;
import software.wings.utils.Misc;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class KubernetesSwapServiceSelectorsTask extends AbstractDelegateRunnableTask {
  @Inject private DelegateLogService delegateLogService;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private KubernetesContainerService kubernetesContainerService;

  private static final Logger logger = LoggerFactory.getLogger(KubernetesSwapServiceSelectorsTask.class);

  public KubernetesSwapServiceSelectorsTask(String delegateId, DelegateTask delegateTask,
      Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public KubernetesSwapServiceSelectorsResponse run(Object[] parameters) {
    KubernetesSwapServiceSelectorsParams kubernetesSwapServiceSelectorsParams =
        (KubernetesSwapServiceSelectorsParams) parameters[0];

    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(delegateLogService,
        kubernetesSwapServiceSelectorsParams.getAccountId(), kubernetesSwapServiceSelectorsParams.getAppId(),
        kubernetesSwapServiceSelectorsParams.getActivityId(), kubernetesSwapServiceSelectorsParams.getCommandName());

    executionLogCallback.saveExecutionLog(
        String.format("Begin execution of command %s", KUBERNETES_SWAP_SERVICE_SELECTORS_COMMAND_NAME), LogLevel.INFO);

    try {
      KubernetesConfig kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(
          kubernetesSwapServiceSelectorsParams.getContainerServiceParams());
      List<EncryptedDataDetail> encryptedDataDetails = emptyList();

      Service service1 = null;
      Service service2 = null;
      service1 = kubernetesContainerService.getService(
          kubernetesConfig, encryptedDataDetails, kubernetesSwapServiceSelectorsParams.getService1());
      service2 = kubernetesContainerService.getService(
          kubernetesConfig, encryptedDataDetails, kubernetesSwapServiceSelectorsParams.getService2());

      if (service1 == null) {
        executionLogCallback.saveExecutionLog(
            String.format("Service %s not found.", kubernetesSwapServiceSelectorsParams.getService1()), LogLevel.ERROR,
            CommandExecutionStatus.FAILURE);
        return KubernetesSwapServiceSelectorsResponse.builder().executionStatus(ExecutionStatus.FAILED).build();
      }

      if (service2 == null) {
        executionLogCallback.saveExecutionLog(
            String.format("Service %s not found.", kubernetesSwapServiceSelectorsParams.getService2()), LogLevel.ERROR,
            CommandExecutionStatus.FAILURE);
        return KubernetesSwapServiceSelectorsResponse.builder().executionStatus(ExecutionStatus.FAILED).build();
      }

      Map<String, String> serviceOneSelectors = service1.getSpec().getSelector();
      Map<String, String> serviceTwoSelectors = service2.getSpec().getSelector();

      executionLogCallback.saveExecutionLog(
          String.format("%nSelectors for Service One : [name:%s]%n%s", service1.getMetadata().getName(),
              toDisplayYaml(service1.getSpec().getSelector())),
          LogLevel.INFO);

      executionLogCallback.saveExecutionLog(
          String.format("%nSelectors for Service Two : [name:%s]%n%s", service2.getMetadata().getName(),
              toDisplayYaml(service2.getSpec().getSelector())),
          LogLevel.INFO);

      executionLogCallback.saveExecutionLog(String.format("%nSwapping Service Selectors..%n"), LogLevel.INFO);

      service1.getSpec().setSelector(serviceTwoSelectors);
      service2.getSpec().setSelector(serviceOneSelectors);

      Service serviceOneUpdated =
          kubernetesContainerService.createOrReplaceService(kubernetesConfig, encryptedDataDetails, service1);

      Service serviceTwoUpdated =
          kubernetesContainerService.createOrReplaceService(kubernetesConfig, encryptedDataDetails, service2);

      executionLogCallback.saveExecutionLog(
          String.format("%nUpdated Selectors for Service One : [name:%s]%n%s",
              serviceOneUpdated.getMetadata().getName(), toDisplayYaml(serviceOneUpdated.getSpec().getSelector())),
          LogLevel.INFO);

      executionLogCallback.saveExecutionLog(
          String.format("%nnUpdated Selectors for Service Two : [name:%s]%n%s",
              serviceTwoUpdated.getMetadata().getName(), toDisplayYaml(serviceTwoUpdated.getSpec().getSelector())),
          LogLevel.INFO);

      executionLogCallback.saveExecutionLog("Done", LogLevel.INFO, CommandExecutionStatus.SUCCESS);

      return KubernetesSwapServiceSelectorsResponse.builder().executionStatus(ExecutionStatus.SUCCESS).build();
    } catch (WingsException e) {
      Misc.logAllMessages(e, executionLogCallback);
      throw e;
    } catch (Exception e) {
      logger.error("Exception in KubernetesSwapServiceSelectors", e);
      Misc.logAllMessages(e, executionLogCallback);
      executionLogCallback.saveExecutionLog(
          "Exception occurred in kubernetesSwapServiceSelectors", LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      return KubernetesSwapServiceSelectorsResponse.builder().executionStatus(ExecutionStatus.FAILED).build();
    }
  }
}
