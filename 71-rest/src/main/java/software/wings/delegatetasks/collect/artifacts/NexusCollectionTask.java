package software.wings.delegatetasks.collect.artifacts;

import com.google.inject.Inject;

import io.harness.delegate.task.protocol.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.waiter.ListNotifyResponseData;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.config.NexusConfig;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.helpers.ext.nexus.NexusService;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by srinivas on 4/4/17.
 */
public class NexusCollectionTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(NexusCollectionTask.class);

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
      return run((NexusConfig) parameters[0], (List<EncryptedDataDetail>) parameters[1], (String) parameters[2],
          (String) parameters[3], (List<String>) parameters[4], (String) parameters[5]);
    } catch (Exception e) {
      logger.error("Exception occurred while collecting artifact", e);
      return new ListNotifyResponseData();
    }
  }

  public ListNotifyResponseData run(NexusConfig nexusConfig, List<EncryptedDataDetail> encryptionDetails,
      String repoType, String groupId, List<String> artifactPaths, String version) {
    ListNotifyResponseData res = new ListNotifyResponseData();
    try {
      for (String artifactPath : artifactPaths) {
        logger.info("Collecting artifact {}  from Nexus server {}", artifactPath, nexusConfig.getNexusUrl());
        nexusService.downloadArtifacts(nexusConfig, encryptionDetails, repoType, groupId, artifactPath, version,
            getDelegateId(), getTaskId(), getAccountId(), res);
      }
    } catch (Exception e) {
      logger.warn("Exception: " + ExceptionUtils.getMessage(e), e);
    }
    return res;
  }
}
