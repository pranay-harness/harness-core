package software.wings.delegatetasks;

import static io.harness.k8s.KubernetesHelperService.toDisplayYaml;

import static software.wings.sm.states.KubernetesSwapServiceSelectors.KUBERNETES_SWAP_SERVICE_SELECTORS_COMMAND_NAME;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.WingsException;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.logging.Misc;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.container.KubernetesSwapServiceSelectorsParams;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.sm.states.KubernetesSwapServiceSelectorsResponse;

import com.google.inject.Inject;
import io.fabric8.kubernetes.api.model.Service;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
@Slf4j
public class KubernetesSwapServiceSelectorsTask extends AbstractDelegateRunnableTask {
  @Inject private DelegateLogService delegateLogService;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private KubernetesContainerService kubernetesContainerService;

  public KubernetesSwapServiceSelectorsTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public KubernetesSwapServiceSelectorsResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
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

      Service service1 = null;
      Service service2 = null;
      service1 = kubernetesContainerService.getServiceFabric8(
          kubernetesConfig, kubernetesSwapServiceSelectorsParams.getService1());
      service2 = kubernetesContainerService.getServiceFabric8(
          kubernetesConfig, kubernetesSwapServiceSelectorsParams.getService2());

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

      Service serviceOneUpdated = kubernetesContainerService.createOrReplaceService(kubernetesConfig, service1);

      Service serviceTwoUpdated = kubernetesContainerService.createOrReplaceService(kubernetesConfig, service2);

      executionLogCallback.saveExecutionLog(
          String.format("%nUpdated Selectors for Service One : [name:%s]%n%s",
              serviceOneUpdated.getMetadata().getName(), toDisplayYaml(serviceOneUpdated.getSpec().getSelector())),
          LogLevel.INFO);

      executionLogCallback.saveExecutionLog(
          String.format("%nUpdated Selectors for Service Two : [name:%s]%n%s",
              serviceTwoUpdated.getMetadata().getName(), toDisplayYaml(serviceTwoUpdated.getSpec().getSelector())),
          LogLevel.INFO);

      executionLogCallback.saveExecutionLog("Done", LogLevel.INFO, CommandExecutionStatus.SUCCESS);

      return KubernetesSwapServiceSelectorsResponse.builder().executionStatus(ExecutionStatus.SUCCESS).build();
    } catch (WingsException e) {
      Misc.logAllMessages(e, executionLogCallback);
      throw e;
    } catch (Exception e) {
      log.error("Exception in KubernetesSwapServiceSelectors", e);
      Misc.logAllMessages(e, executionLogCallback);
      executionLogCallback.saveExecutionLog(
          "Exception occurred in kubernetesSwapServiceSelectors", LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      return KubernetesSwapServiceSelectorsResponse.builder().executionStatus(ExecutionStatus.FAILED).build();
    }
  }
}
