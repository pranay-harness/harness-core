package io.harness.delegate.task.scm;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.scm.ScmOnDelegateResponse;
import io.harness.delegate.beans.scm.ScmOnDelegateTaskParams;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.product.ci.scm.proto.CreateFileResponse;
import io.harness.service.ScmClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ScmDelegateClientImpl extends AbstractDelegateRunnableTask implements ScmDelegateClient {
  @Inject private ScmClient scmClient;
  @Inject private ScmDelegateClient scmDelegateClient;

  public ScmDelegateClientImpl(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public <T> T processScmRequest(Callable<T> callable) {
    try {
      ScmUnixManager scmUnixManager = null;
      try {
        scmUnixManager = new ScmUnixManager();
        final T call = callable.call();
        // (Todo) : Close SCM Manager in background
        scmUnixManager.close();
        return call;
      } catch (IOException ioException) {
        // handle it here
        return null;
      }
    } catch (Exception e) {
      throw new InvalidRequestException("");
    }
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    return null;
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    ScmOnDelegateTaskParams scmOnDelegateTaskParams = (ScmOnDelegateTaskParams) parameters;
    final CreateFileResponse createFileResponse =
        scmDelegateClient.processScmRequest(()
                                                -> scmClient.createFile(scmOnDelegateTaskParams.getScmConnector(),
                                                    scmOnDelegateTaskParams.getGitFileDetails()));
    return ScmOnDelegateResponse.builder().createFileResponse(createFileResponse).build();
  }
}
