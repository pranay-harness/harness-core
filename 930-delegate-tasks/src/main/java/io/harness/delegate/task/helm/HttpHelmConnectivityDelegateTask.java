package io.harness.delegate.task.helm;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectivityTaskParams;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectivityTaskResponse;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectorDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmValidationParams;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class HttpHelmConnectivityDelegateTask extends AbstractDelegateRunnableTask {
  @Inject private HttpHelmValidationHandler httpHelmValidationHandler;

  public HttpHelmConnectivityDelegateTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    HttpHelmConnectivityTaskParams httpHelmConnectivityTaskParams = (HttpHelmConnectivityTaskParams) parameters;
    HttpHelmConnectorDTO httpHelmConnectorDTO = httpHelmConnectivityTaskParams.getHelmConnector();
    final HttpHelmValidationParams httpHelmValidationParams =
        HttpHelmValidationParams.builder()
            .encryptionDataDetails(httpHelmConnectivityTaskParams.getEncryptionDetails())
            .httpHelmConnectorDTO(httpHelmConnectorDTO)
            .build();
    ConnectorValidationResult httpHelmConnectorValidationResult =
        httpHelmValidationHandler.validate(httpHelmValidationParams, getAccountId());
    httpHelmConnectorValidationResult.setDelegateId(getDelegateId());
    return HttpHelmConnectivityTaskResponse.builder()
        .connectorValidationResult(httpHelmConnectorValidationResult)
        .build();
  }

  @Override
  @Deprecated
  public DelegateResponseData run(Object[] parameters) {
    throw new UnsupportedOperationException("This method is deprecated. Use run(TaskParameters) instead.");
  }
}
