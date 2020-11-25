package io.harness.delegate.task.artifacts.docker;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.logging.CommandExecutionStatus;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class DockerArtifactTaskNG extends AbstractDelegateRunnableTask {
  @Inject DockerArtifactTaskHelper dockerArtifactTaskHelper;

  public DockerArtifactTaskNG(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public ArtifactTaskResponse run(TaskParameters parameters) {
    try {
      ArtifactTaskParameters taskParameters = (ArtifactTaskParameters) parameters;
      return dockerArtifactTaskHelper.getArtifactCollectResponse(taskParameters);
    } catch (Exception exception) {
      log.error("Exception in processing DockerArtifactTaskNG task [{}]", exception);
      return ArtifactTaskResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(exception))
          .errorCode(ErrorCode.INVALID_ARGUMENT)
          .build();
    }
  }
}
