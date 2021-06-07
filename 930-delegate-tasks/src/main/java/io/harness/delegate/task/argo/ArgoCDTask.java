package io.harness.delegate.task.argo;

import io.harness.argo.beans.ClusterResourceTreeDTO;
import io.harness.argo.service.ArgoCdService;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.argo.request.ArgoRequest;
import io.harness.delegate.beans.argo.response.ResourceTreeResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ArgoCDTask extends AbstractDelegateRunnableTask {
  @Inject private ArgoCdService argoCdService;
  public ArgoCDTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new UnsupportedOperationException("not supported");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    ArgoRequest request = (ArgoRequest) parameters;
    switch (request.requestType()) {
      case RESOURCE_TREE:
        final ClusterResourceTreeDTO clusterResourceTreeDTO;
        try {
          clusterResourceTreeDTO =
              argoCdService.fetchResourceTree(request.getArgoConfigInternal(), request.getAppName());
          return ResourceTreeResponse.builder()
              .executionStatus(CommandExecutionStatus.SUCCESS)
              .clusterResourceTree(clusterResourceTreeDTO)
              .build();
        } catch (IOException e) {
          return ResourceTreeResponse.builder()
              .executionStatus(CommandExecutionStatus.SUCCESS)
              .errorMessage(e.getMessage())
              .build();
        }
      case APP_SYNC:
      case MANIFEST_DIFF:
      default:
        throw new InvalidRequestException("Unhandled Argo TaskType");
    }
  }
}
