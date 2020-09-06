package software.wings.delegatetasks.collect.artifacts;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.ListNotifyResponseData;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.helpers.ext.artifactory.ArtifactoryService;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Created by srinivas on 4/4/17.
 */
@OwnedBy(CDC)
@Slf4j
public class ArtifactoryCollectionTask extends AbstractDelegateRunnableTask {
  @Inject private ArtifactoryService artifactoryService;

  public ArtifactoryCollectionTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> postExecute, BooleanSupplier preExecute) {
    super(delegateTaskPackage, postExecute, preExecute);
  }

  @Override
  public ListNotifyResponseData run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public ListNotifyResponseData run(Object[] parameters) {
    try {
      return run((ArtifactoryConfig) parameters[0], (List<EncryptedDataDetail>) parameters[1], (String) parameters[2],
          (Map<String, String>) parameters[3]);
    } catch (Exception e) {
      logger.error("Exception occurred while collecting artifact", e);
      return new ListNotifyResponseData();
    }
  }

  public ListNotifyResponseData run(ArtifactoryConfig artifactoryConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String repositoryName, Map<String, String> metadata) {
    try {
      return artifactoryService.downloadArtifacts(artifactoryConfig, encryptedDataDetails, repositoryName, metadata,
          getDelegateId(), getTaskId(), getAccountId());
    } catch (Exception e) {
      logger.warn("Exception occurred while collecting artifact for artifact server {} : {}",
          artifactoryConfig.getArtifactoryUrl(), ExceptionUtils.getMessage(e), e);
    }
    return new ListNotifyResponseData();
  }
}
