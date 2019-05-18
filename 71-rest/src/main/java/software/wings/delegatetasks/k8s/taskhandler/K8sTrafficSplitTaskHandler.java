package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.exception.ExceptionUtils.getMessage;
import static java.util.Collections.emptyList;
import static software.wings.beans.Log.LogColor.White;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.color;
import static software.wings.beans.command.K8sDummyCommandUnit.Init;
import static software.wings.beans.command.K8sDummyCommandUnit.TrafficSplit;
import static software.wings.sm.states.k8s.K8sTrafficSplitState.K8S_VIRTUAL_SERVICE_PLACEDOLDER;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.model.Kind;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.ReleaseHistory;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.k8s.istio.IstioDestinationWeight;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTrafficSplitTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.helpers.ext.k8s.response.K8sTrafficSplitResponse;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
@Slf4j
public class K8sTrafficSplitTaskHandler extends K8sTaskHandler {
  @Inject private KubernetesContainerService kubernetesContainerService;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private K8sTaskHelper k8sTaskHelper;

  public static final String ISTIO_DESTINATION_TEMPLATE = "host: $ISTIO_DESTINATION_HOST_NAME\n"
      + "subset: $ISTIO_DESTINATION_SUBSET_NAME";

  private ReleaseHistory releaseHistory;
  private Release release;
  private KubernetesConfig kubernetesConfig;
  private VirtualService virtualService;

  public K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) {
    if (!(k8sTaskParameters instanceof K8sTrafficSplitTaskParameters)) {
      throw new InvalidArgumentsException(
          Pair.of("k8sTaskParameters", "Must be instance of K8sTrafficSplitTaskParameters"));
    }

    K8sTrafficSplitTaskParameters k8sTrafficSplitTaskParameters = (K8sTrafficSplitTaskParameters) k8sTaskParameters;
    K8sTrafficSplitResponse k8sTrafficSplitResponse = K8sTrafficSplitResponse.builder().build();

    boolean success = init(k8sTrafficSplitTaskParameters,
        new ExecutionLogCallback(delegateLogService, k8sTrafficSplitTaskParameters.getAccountId(),
            k8sTrafficSplitTaskParameters.getAppId(), k8sTrafficSplitTaskParameters.getActivityId(), Init));

    if (!success) {
      return k8sTaskHelper.getK8sTaskExecutionResponse(k8sTrafficSplitResponse, CommandExecutionStatus.FAILURE);
    }

    success = apply(k8sTrafficSplitTaskParameters,
        new ExecutionLogCallback(delegateLogService, k8sTrafficSplitTaskParameters.getAccountId(),
            k8sTrafficSplitTaskParameters.getAppId(), k8sTrafficSplitTaskParameters.getActivityId(), TrafficSplit));

    if (!success) {
      return k8sTaskHelper.getK8sTaskExecutionResponse(k8sTrafficSplitResponse, CommandExecutionStatus.FAILURE);
    }

    return k8sTaskHelper.getK8sTaskExecutionResponse(k8sTrafficSplitResponse, CommandExecutionStatus.SUCCESS);
  }

  private boolean init(
      K8sTrafficSplitTaskParameters k8sTrafficSplitTaskParameters, ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("Initializing..");

    kubernetesConfig =
        containerDeploymentDelegateHelper.getKubernetesConfig(k8sTrafficSplitTaskParameters.getK8sClusterConfig());

    try {
      boolean success;

      if (K8S_VIRTUAL_SERVICE_PLACEDOLDER.equals(k8sTrafficSplitTaskParameters.getVirtualServiceName())) {
        success = initBasedOnDefaultVirtualServiceName(k8sTrafficSplitTaskParameters, executionLogCallback);
      } else {
        success = initBasedOnCustomVirtualServiceName(k8sTrafficSplitTaskParameters, executionLogCallback);
      }

      if (!success) {
        return false;
      }

      printDestinationWeights(k8sTrafficSplitTaskParameters, executionLogCallback);
      executionLogCallback.saveExecutionLog("\nDone.", INFO, SUCCESS);

      return true;
    } catch (Exception e) {
      logger.error("Exception:", e);
      executionLogCallback.saveExecutionLog(getMessage(e), ERROR);
      executionLogCallback.saveExecutionLog("\nFailed.", ERROR, CommandExecutionStatus.FAILURE);
      return false;
    }
  }

  private boolean initBasedOnCustomVirtualServiceName(
      K8sTrafficSplitTaskParameters k8sTrafficSplitTaskParameters, ExecutionLogCallback executionLogCallback) {
    return findVirtualServiceByName(k8sTrafficSplitTaskParameters.getVirtualServiceName(), executionLogCallback);
  }

  private boolean initBasedOnDefaultVirtualServiceName(K8sTrafficSplitTaskParameters k8sTrafficSplitTaskParameters,
      ExecutionLogCallback executionLogCallback) throws IOException {
    executionLogCallback.saveExecutionLog(
        color("\nRelease name: " + k8sTrafficSplitTaskParameters.getReleaseName(), White, Bold));

    String releaseHistoryData = kubernetesContainerService.fetchReleaseHistory(
        kubernetesConfig, emptyList(), k8sTrafficSplitTaskParameters.getReleaseName());

    if (StringUtils.isEmpty(releaseHistoryData)) {
      executionLogCallback.saveExecutionLog("\nNo release history found for release ");
      executionLogCallback.saveExecutionLog("\nDone.", INFO, SUCCESS);
      return true;
    }

    releaseHistory = ReleaseHistory.createFromData(releaseHistoryData);
    release = releaseHistory.getLatestRelease();

    List<KubernetesResourceId> resources = release.getResources();
    if (isEmpty(resources)) {
      executionLogCallback.saveExecutionLog("\nNo resources found");
      executionLogCallback.saveExecutionLog("\nDone.", INFO, SUCCESS);
      return true;
    }

    List<KubernetesResourceId> virtualServiceResources =
        resources.stream()
            .filter(kubernetesResourceId -> Kind.VirtualService.name().equals(kubernetesResourceId.getKind()))
            .collect(Collectors.toList());

    if (virtualServiceResources.size() != 1) {
      if (virtualServiceResources.isEmpty()) {
        executionLogCallback.saveExecutionLog("\nNo VirtualService found", ERROR, FAILURE);
      } else if (virtualServiceResources.size() > 1) {
        executionLogCallback.saveExecutionLog("\nMore than one VirtualService found", ERROR, FAILURE);
      }

      return false;
    }

    return findVirtualServiceByName(virtualServiceResources.get(0).getName(), executionLogCallback);
  }

  private boolean apply(
      K8sTrafficSplitTaskParameters k8sTrafficSplitTaskParameters, ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("Applying..");

    kubernetesConfig =
        containerDeploymentDelegateHelper.getKubernetesConfig(k8sTrafficSplitTaskParameters.getK8sClusterConfig());

    try {
      updateVirtualServiceWithDestinationWeights(k8sTrafficSplitTaskParameters, executionLogCallback);
      executionLogCallback.saveExecutionLog("\n" + KubernetesHelper.toYaml(virtualService));

      virtualService = (VirtualService) kubernetesContainerService.createOrReplaceIstioResource(
          kubernetesConfig, emptyList(), virtualService);

      executionLogCallback.saveExecutionLog("\nDone.", INFO, SUCCESS);
      return true;
    } catch (Exception e) {
      logger.error("Exception:", e);
      executionLogCallback.saveExecutionLog(getMessage(e), ERROR);
      executionLogCallback.saveExecutionLog("\nFailed.", ERROR, CommandExecutionStatus.FAILURE);
      return false;
    }
  }

  private boolean findVirtualServiceByName(String virtualServiceName, ExecutionLogCallback executionLogCallback) {
    virtualService =
        kubernetesContainerService.getIstioVirtualService(kubernetesConfig, emptyList(), virtualServiceName);
    if (virtualService == null) {
      executionLogCallback.saveExecutionLog(
          "\nNo VirtualService found with name " + virtualServiceName, ERROR, FAILURE);
      return false;
    }

    executionLogCallback.saveExecutionLog("\nFound VirtualService with name " + color(virtualServiceName, White, Bold));

    return true;
  }

  private void updateVirtualServiceWithDestinationWeights(K8sTrafficSplitTaskParameters k8sTrafficSplitTaskParameters,
      ExecutionLogCallback executionLogCallback) throws IOException {
    List<IstioDestinationWeight> istioDestinationWeights = k8sTrafficSplitTaskParameters.getIstioDestinationWeights();

    k8sTaskHelper.updateVirtualServiceWithDestinationWeights(
        istioDestinationWeights, virtualService, executionLogCallback);
  }

  private void printDestinationWeights(
      K8sTrafficSplitTaskParameters k8sTrafficSplitTaskParameters, ExecutionLogCallback executionLogCallback) {
    List<IstioDestinationWeight> istioDestinationWeights = k8sTrafficSplitTaskParameters.getIstioDestinationWeights();

    if (isNotEmpty(istioDestinationWeights)) {
      executionLogCallback.saveExecutionLog("\nFound following destinations");
      for (IstioDestinationWeight ruleWithWeight : istioDestinationWeights) {
        executionLogCallback.saveExecutionLog(ruleWithWeight.getDestination());
        executionLogCallback.saveExecutionLog("weight: " + ruleWithWeight.getWeight() + "\n");
      }
    }
  }
}
