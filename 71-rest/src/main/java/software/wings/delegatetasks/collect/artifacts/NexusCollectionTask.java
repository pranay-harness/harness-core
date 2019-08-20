package software.wings.delegatetasks.collect.artifacts;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.waiter.ListNotifyResponseData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.config.NexusConfig;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.helpers.ext.nexus.NexusService;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by srinivas on 4/4/17.
 */
@Slf4j
public class NexusCollectionTask extends AbstractDelegateRunnableTask {
  @Inject private NexusService nexusService;

  @Inject private DelegateFileManager delegateFileManager;

  @Inject private ArtifactCollectionTaskHelper artifactCollectionTaskHelper;

  public NexusCollectionTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public ListNotifyResponseData run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public ListNotifyResponseData run(Object[] parameters) {
    try {
      return run((NexusConfig) parameters[0], (List<EncryptedDataDetail>) parameters[1],
          (ArtifactStreamAttributes) parameters[2], (Map<String, String>) parameters[3]);
    } catch (Exception e) {
      logger.error("Exception occurred while collecting artifact", e);
      return new ListNotifyResponseData();
    }
  }

  public ListNotifyResponseData run(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes, Map<String, String> artifactMetadata) {
    ListNotifyResponseData res = new ListNotifyResponseData();
    try {
      logger.info("Collecting artifact {}  from Nexus server {}", nexusConfig.getNexusUrl());
      nexusService.downloadArtifacts(nexusConfig, encryptionDetails, artifactStreamAttributes, artifactMetadata,
          getDelegateId(), getTaskId(), getAccountId(), res);

    } catch (Exception e) {
      logger.warn("Exception: " + ExceptionUtils.getMessage(e), e);
    }
    return res;
  }
}
