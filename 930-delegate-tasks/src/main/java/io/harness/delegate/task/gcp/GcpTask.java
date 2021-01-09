package io.harness.delegate.task.gcp;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.gcp.request.GcpRequest;
import io.harness.delegate.task.gcp.request.GcpRequest.RequestType;
import io.harness.delegate.task.gcp.response.GcpResponse;
import io.harness.delegate.task.gcp.response.GcpValidationTaskResponse;
import io.harness.delegate.task.gcp.taskHandlers.TaskHandler;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import com.google.inject.Inject;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class GcpTask extends AbstractDelegateRunnableTask {
  @Inject private Map<RequestType, TaskHandler> gcpTaskTypeToTaskHandlerMap;

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
    if (!(parameters instanceof GcpRequest)) {
      throw new InvalidRequestException("Task Params are not of expected type: GcpRequest");
    }
    final GcpRequest gcpRequest = (GcpRequest) parameters;
    final RequestType requestType = gcpRequest.getRequestType();

    switch (requestType) {
      case VALIDATE:
        GcpResponse gcpResponse = gcpTaskTypeToTaskHandlerMap.get(requestType).executeRequest(gcpRequest);
        ConnectorValidationResult connectorValidationResult =
            ((GcpValidationTaskResponse) gcpResponse).getConnectorValidationResult();
        connectorValidationResult.setDelegateId(getDelegateId());
        return gcpResponse;
      default:
        throw new InvalidRequestException(
            "Invalid request type [" + gcpRequest.getRequestType() + "]", WingsException.USER);
    }
  }
}
