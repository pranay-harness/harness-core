package software.wings.delegatetasks.collect.artifacts;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.waiter.ListNotifyResponseData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.BambooConfig;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.helpers.ext.bamboo.BambooService;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by rishi on 12/14/16.
 */
@Slf4j
public class BambooCollectionTask extends AbstractDelegateRunnableTask {
  @Inject private BambooService bambooService;
  @Inject private ArtifactCollectionTaskHelper artifactCollectionTaskHelper;

  public BambooCollectionTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public ListNotifyResponseData run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public ListNotifyResponseData run(Object[] parameters) {
    return run((BambooConfig) parameters[0], (List<EncryptedDataDetail>) parameters[1],
        (ArtifactStreamAttributes) parameters[2], (Map<String, String>) parameters[3]);
  }

  public ListNotifyResponseData run(BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes, Map<String, String> arguments) {
    ListNotifyResponseData res = new ListNotifyResponseData();
    try {
      bambooService.downloadArtifacts(bambooConfig, encryptionDetails, artifactStreamAttributes,
          arguments.get(ArtifactMetadataKeys.buildNo), getDelegateId(), getTaskId(), getAccountId(), res);
    } catch (Exception e) {
      logger.warn("Exception: " + ExceptionUtils.getMessage(e), e);
      // TODO: better error handling

      //      if (e instanceof WingsException)
      //        WingsException ex = (WingsException) e;
      //        errorMessage = Joiner.on(",").join(ex.getResponseMessageList().stream()
      //            .map(responseMessage ->
      //            MessageManager.getInstance().getResponseMessage(responseMessage.getCode(),
      //            ex.getParams()).getMessage()) .collect(toList()));
      //      } else {
      //        errorMessage = e.getMessage();
      //      }
      //      executionStatus = executionStatus.FAILED;
      //      jenkinsExecutionResponse.setErrorMessage(errorMessage);
    }

    return res;
  }
}
