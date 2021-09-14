/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks.collect.artifacts;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.ListNotifyResponseData;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;

import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.settings.azureartifacts.AzureArtifactsConfig;
import software.wings.helpers.ext.azure.devops.AzureArtifactsService;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;

@OwnedBy(CDC)
@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class AzureArtifactsCollectionTask extends AbstractDelegateRunnableTask {
  @Inject private AzureArtifactsService azureArtifactsService;

  public AzureArtifactsCollectionTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> postExecute,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, postExecute, preExecute);
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
      log.info(format("Collecting artifact: [%s] from Azure Artifacts xxxxxxxx [%s]", version,
          azureArtifactsConfig.getAzureDevopsUrl()));
      azureArtifactsService.downloadArtifact(azureArtifactsConfig, taskParameters.getEncryptedDataDetails(),
          taskParameters.getArtifactStreamAttributes(), artifactMetadata, getDelegateId(), getTaskId(), getAccountId(),
          res);
    } catch (Exception e) {
      log.warn("Exception: " + ExceptionUtils.getMessage(e), e);
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
      log.error("Exception occurred while collecting artifact", e);
      return new ListNotifyResponseData();
    }
  }
}
