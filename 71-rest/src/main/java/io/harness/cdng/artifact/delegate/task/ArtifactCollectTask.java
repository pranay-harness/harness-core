package io.harness.cdng.artifact.delegate.task;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.cdng.artifact.bean.ArtifactAttributes;
import io.harness.cdng.artifact.delegate.DelegateArtifactService;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class ArtifactCollectTask extends AbstractDelegateRunnableTask {
  @Inject private Injector injector;

  public ArtifactCollectTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public ResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public ResponseData run(TaskParameters parameters) {
    try {
      ArtifactTaskParameters taskParameters = (ArtifactTaskParameters) parameters;
      DelegateArtifactService artifactService =
          injector.getInstance(taskParameters.getAttributes().getDelegateArtifactService());
      ArtifactAttributes artifactAttributes = artifactService.getLastSuccessfulBuild(
          taskParameters.getAppId(), taskParameters.getAttributes(), taskParameters.getConnectorConfig());
      return ArtifactTaskResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
          .artifactAttributes(artifactAttributes)
          .build();
    } catch (Exception exception) {
      logger.error("Exception in processing ArtifactCollectTask task [{}]", exception);
      return ArtifactTaskResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(exception))
          .build();
    }
  }
}
