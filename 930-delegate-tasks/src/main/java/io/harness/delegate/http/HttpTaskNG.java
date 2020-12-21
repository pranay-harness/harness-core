package io.harness.delegate.http;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.http.HttpStepResponse;
import io.harness.delegate.task.http.HttpTaskParametersNg;
import io.harness.http.HttpService;
import io.harness.http.beans.HttpInternalConfig;
import io.harness.http.beans.HttpInternalResponse;
import io.harness.logging.CommandExecutionStatus;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
@TargetModule(Module._930_DELEGATE_TASKS)
public class HttpTaskNG extends AbstractDelegateRunnableTask {
  @Inject private HttpService httpService;

  public HttpTaskNG(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> postExecute, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, postExecute, preExecute);
  }

  @Override
  public HttpStepResponse run(TaskParameters parameters) {
    HttpTaskParametersNg httpTaskParametersNg = (HttpTaskParametersNg) parameters;
    // Todo: Need to look into useProxy and isCertValidationRequired Field.
    HttpInternalResponse httpInternalResponse =
        httpService.executeUrl(HttpInternalConfig.builder()
                                   .method(httpTaskParametersNg.getMethod())
                                   .body(httpTaskParametersNg.getBody())
                                   .header(null)
                                   .requestHeaders(httpTaskParametersNg.getRequestHeader())
                                   .socketTimeoutMillis(httpTaskParametersNg.getSocketTimeoutMillis())
                                   .url(httpTaskParametersNg.getUrl())
                                   .useProxy(true)
                                   .isCertValidationRequired(false)
                                   .build());
    return HttpStepResponse.builder()
        .commandExecutionStatus(httpInternalResponse.getCommandExecutionStatus())
        .errorMessage(httpInternalResponse.getErrorMessage())
        .header(httpInternalResponse.getHeader())
        .httpMethod(httpInternalResponse.getHttpMethod())
        .httpUrl(httpInternalResponse.getHttpUrl())
        .httpResponseCode(httpInternalResponse.getHttpResponseCode())
        .httpResponseBody(httpInternalResponse.getHttpResponseBody())
        .build();
  }

  @Override
  public HttpStepResponse run(Object[] parameters) {
    throw new NotImplementedException("Not implemented");
  }
}
