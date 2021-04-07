package io.harness.delegate.task.gcp;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.gcp.request.GcpRequest;
import io.harness.delegate.task.gcp.request.GcpTaskParameters;
import io.harness.delegate.task.gcp.response.GcpResponse;
import io.harness.delegate.task.gcp.response.GcpValidationTaskResponse;
import io.harness.delegate.task.gcp.taskHandlers.GcpListClustersTaskHandler;
import io.harness.delegate.task.gcp.taskHandlers.TaskHandler;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import com.google.inject.Inject;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@OwnedBy(CDP)
public class GcpTask extends AbstractDelegateRunnableTask {
  @Inject private Map<GcpTaskType, TaskHandler> gcpTaskTypeToTaskHandlerMap;

  public GcpTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new UnsupportedOperationException("Object Array parameters not supported");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    if (!(parameters instanceof GcpTaskParameters)) {
      throw new InvalidRequestException("Task Params are not of expected type: GcpTaskParameters");
    }
    final GcpTaskParameters gcpTaskParameters = (GcpTaskParameters) parameters;
    final GcpRequest gcpRequest = gcpTaskParameters.getGcpRequest();

    switch (gcpTaskParameters.getGcpTaskType()) {
      case VALIDATE:
        GcpResponse gcpResponse =
            gcpTaskTypeToTaskHandlerMap.get(gcpTaskParameters.getGcpTaskType()).executeRequest(gcpRequest);
        ConnectorValidationResult connectorValidationResult =
            ((GcpValidationTaskResponse) gcpResponse).getConnectorValidationResult();
        connectorValidationResult.setDelegateId(getDelegateId());
        return gcpResponse;

      case LIST_CLUSTERS:
        GcpListClustersTaskHandler gcpListClustersTaskHandler =
            (GcpListClustersTaskHandler) gcpTaskTypeToTaskHandlerMap.get(gcpTaskParameters.getGcpTaskType());
        return gcpListClustersTaskHandler.executeRequest(gcpRequest);

      default:
        throw new InvalidRequestException(
            "Invalid request type [" + gcpTaskParameters.getGcpTaskType() + "]", WingsException.USER);
    }
  }
}
