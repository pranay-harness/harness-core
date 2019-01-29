package software.wings.delegatetasks.k8s.taskhandler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.k8s.model.KubernetesResourceId.createKubernetesResourceIdsFromKindName;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.beans.command.K8sDummyCommandUnit.Delete;
import static software.wings.beans.command.K8sDummyCommandUnit.Init;
import static software.wings.delegatetasks.k8s.K8sTaskHelper.getResourcesInStringFormat;

import com.google.inject.Inject;

import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.KubernetesResourceId;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.k8s.K8sDelegateTaskParams;
import software.wings.delegatetasks.k8s.K8sTaskHelper;
import software.wings.helpers.ext.k8s.request.K8sDeleteTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sDeleteResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.utils.Misc;

import java.util.List;

@NoArgsConstructor
public class K8sDeleteTaskHandler extends K8sTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(K8sDeleteTaskHandler.class);
  @Inject private transient K8sTaskHelper k8sTaskHelper;

  private Kubectl client;
  private List<KubernetesResourceId> resourceIdsToDelete;

  public K8sTaskExecutionResponse executeTaskInternal(
      K8sTaskParameters k8sTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    if (!(k8sTaskParameters instanceof K8sDeleteTaskParameters)) {
      throw new InvalidArgumentsException(Pair.of("k8sTaskParameters", "Must be instance of K8sDeleteTaskParameters"));
    }

    K8sDeleteTaskParameters k8sDeleteTaskParameters = (K8sDeleteTaskParameters) k8sTaskParameters;

    boolean success = init(k8sDeleteTaskParameters, k8sDelegateTaskParams,
        new ExecutionLogCallback(delegateLogService, k8sDeleteTaskParameters.getAccountId(),
            k8sDeleteTaskParameters.getAppId(), k8sDeleteTaskParameters.getActivityId(), Init));

    if (!success) {
      return k8sTaskHelper.getK8sTaskExecutionResponse(
          K8sDeleteResponse.builder().build(), CommandExecutionStatus.FAILURE);
    }

    if (isEmpty(resourceIdsToDelete)) {
      return k8sTaskHelper.getK8sTaskExecutionResponse(
          K8sDeleteResponse.builder().build(), CommandExecutionStatus.SUCCESS);
    }

    k8sTaskHelper.delete(client, k8sDelegateTaskParams, resourceIdsToDelete,
        new ExecutionLogCallback(delegateLogService, k8sDeleteTaskParameters.getAccountId(),
            k8sDeleteTaskParameters.getAppId(), k8sDeleteTaskParameters.getActivityId(), Delete));

    return k8sTaskHelper.getK8sTaskExecutionResponse(
        K8sDeleteResponse.builder().build(), CommandExecutionStatus.SUCCESS);
  }

  private boolean init(K8sDeleteTaskParameters k8sDeleteTaskParameters, K8sDelegateTaskParams k8sDelegateTaskParams,
      ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("Initializing..\n");

    try {
      client = Kubectl.client(k8sDelegateTaskParams.getKubectlPath(), k8sDelegateTaskParams.getKubeconfigPath());

      if (StringUtils.isEmpty(k8sDeleteTaskParameters.getResources())) {
        executionLogCallback.saveExecutionLog("\nNo resources found to delete.");
        executionLogCallback.saveExecutionLog("\nDone.", INFO, SUCCESS);
        return true;
      }

      resourceIdsToDelete = createKubernetesResourceIdsFromKindName(k8sDeleteTaskParameters.getResources());

      executionLogCallback.saveExecutionLog(
          "\nWorkloads to delete are: " + getResourcesInStringFormat(resourceIdsToDelete));

      executionLogCallback.saveExecutionLog("Done.", INFO, SUCCESS);

      return true;
    } catch (Exception e) {
      executionLogCallback.saveExecutionLog(Misc.getMessage(e), ERROR);
      executionLogCallback.saveExecutionLog("\nFailed.", ERROR, CommandExecutionStatus.FAILURE);
      return false;
    }
  }
}
