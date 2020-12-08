package software.wings.delegatetasks;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.http.HttpService;
import io.harness.http.beans.HttpInternalConfig;
import io.harness.http.beans.HttpInternalResponse;

import software.wings.sm.states.HttpState.HttpStateExecutionResponse;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
@Slf4j
@TargetModule(Module._930_DELEGATE_TASKS)
public class HttpTask extends AbstractDelegateRunnableTask {
  @Inject private HttpService httpService;

  public HttpTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> postExecute, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, postExecute, preExecute);
  }

  @Override
  public HttpStateExecutionResponse run(TaskParameters parameters) {
    HttpTaskParameters httpTaskParameters = (HttpTaskParameters) parameters;
    HttpInternalResponse httpInternalResponse =
        httpService.executeUrl(HttpInternalConfig.builder()
                                   .method(httpTaskParameters.getMethod())
                                   .body(httpTaskParameters.getBody())
                                   .header(httpTaskParameters.getHeader())
                                   .socketTimeoutMillis(httpTaskParameters.getSocketTimeoutMillis())
                                   .url(httpTaskParameters.getUrl())
                                   .useProxy(httpTaskParameters.isUseProxy())
                                   .isCertValidationRequired(httpTaskParameters.isCertValidationRequired())
                                   .build());
    return HttpStateExecutionResponse.builder()
        .executionStatus(
            ExecutionStatus.translateCommandExecutionStatus(httpInternalResponse.getCommandExecutionStatus()))
        .errorMessage(httpInternalResponse.getErrorMessage())
        .header(httpInternalResponse.getHeader())
        .httpMethod(httpInternalResponse.getHttpMethod())
        .httpUrl(httpInternalResponse.getHttpUrl())
        .httpResponseCode(httpInternalResponse.getHttpResponseCode())
        .httpResponseBody(httpInternalResponse.getHttpResponseBody())
        .build();
  }

  @Override
  public HttpStateExecutionResponse run(Object[] parameters) {
    throw new NotImplementedException("Not implemented");
  }
}
