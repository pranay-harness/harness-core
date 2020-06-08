package software.wings.delegatetasks.collect.artifacts;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.waiter.ListNotifyResponseData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.settings.azureartifacts.AzureArtifactsConfig;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.helpers.ext.azure.devops.AzureArtifactsService;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@OwnedBy(CDC)
@Slf4j
public class AzureArtifactsCollectionTask extends AbstractDelegateRunnableTask {
  @Inject private AzureArtifactsService azureArtifactsService;

  public AzureArtifactsCollectionTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> postExecute, BooleanSupplier preExecute) {
    super(delegateTaskPackage, postExecute, preExecute);
  }

  @Override
  public ListNotifyResponseData run(TaskParameters parameters) {
    AzureArtifactsCollectionTaskParameters taskParameters = (AzureArtifactsCollectionTaskParameters) parameters;
    ListNotifyResponseData res = new ListNotifyResponseData();
    try {
      Map<String, String> artifactMetadata = taskParameters.getArtifactMetadata();
      if (artifactMetadata == null) {
        artifactMetadata = new HashMap<>();
      }
      String version = artifactMetadata.getOrDefault(ArtifactMetadataKeys.version, null);
      if (version == null) {
        version = "null";
      }

      AzureArtifactsConfig azureArtifactsConfig = taskParameters.getAzureArtifactsConfig();
      logger.info(format("Collecting artifact: [%s] from Azure Artifacts xxxxxxxx [%s]", version,
          azureArtifactsConfig.getAzureDevopsUrl()));
      azureArtifactsService.downloadArtifact(azureArtifactsConfig, taskParameters.getEncryptedDataDetails(),
          taskParameters.getArtifactStreamAttributes(), artifactMetadata, getDelegateId(), getTaskId(), getAccountId(),
          res);
    } catch (Exception e) {
      logger.warn("Exception: " + ExceptionUtils.getMessage(e), e);
    }
    return res;
  }

  @Override
  public ListNotifyResponseData run(Object[] parameters) {
    try {
      if (EmptyPredicate.isEmpty(parameters) || !(parameters[0] instanceof TaskParameters)) {
        throw new InvalidArgumentsException(ImmutablePair.of("args", "Invalid task parameters"));
      }
      return run((TaskParameters) parameters[0]);
    } catch (Exception e) {
      logger.error("Exception occurred while collecting artifact", e);
      return new ListNotifyResponseData();
    }
  }
}
