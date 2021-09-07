package io.harness.polling;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.polling.contracts.Type.GCS_HELM;
import static io.harness.polling.contracts.Type.HTTP_HELM;
import static io.harness.polling.contracts.Type.S3_HELM;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.GcsStoreConfig;
import io.harness.cdng.manifest.yaml.HttpStoreConfig;
import io.harness.cdng.manifest.yaml.S3StoreConfig;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.polling.ManifestPollingDelegateResponse;
import io.harness.delegate.beans.polling.PollingDelegateResponse;
import io.harness.delegate.beans.polling.PollingResponseInfc;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.polling.bean.PolledResponseResult;
import io.harness.polling.bean.PollingDocument;
import io.harness.polling.bean.artifact.ArtifactInfo;
import io.harness.polling.bean.manifest.HelmChartManifestInfo;
import io.harness.polling.bean.manifest.ManifestInfo;
import io.harness.polling.bean.manifest.ManifestPolledResponse;
import io.harness.polling.contracts.BuildInfo;
import io.harness.polling.contracts.PollingResponse;
import io.harness.polling.service.PolledItemPublisher;
import io.harness.polling.service.intfc.PollingPerpetualTaskService;
import io.harness.polling.service.intfc.PollingService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.CDC)
public class PollingResponseHandler {
  private static final int MAX_FAILED_ATTEMPTS = 3500;
  private PollingService pollingService;
  private PollingPerpetualTaskService pollingPerpetualTaskService;
  private PolledItemPublisher polledItemPublisher;

  @Inject
  public PollingResponseHandler(PollingService pollingService, PollingPerpetualTaskService pollingPerpetualTaskService,
      PolledItemPublisher polledItemPublisher) {
    this.pollingService = pollingService;
    this.pollingPerpetualTaskService = pollingPerpetualTaskService;
    this.polledItemPublisher = polledItemPublisher;
  }

  public void handlePollingResponse(
      @NotEmpty String perpetualTaskId, @NotEmpty String accountId, PollingDelegateResponse executionResponse) {
    String pollDocId = executionResponse.getPollingDocId();
    PollingDocument pollingDocument = pollingService.get(accountId, pollDocId);
    if (pollingDocument == null || !perpetualTaskId.equals(pollingDocument.getPerpetualTaskId())) {
      pollingPerpetualTaskService.deletePerpetualTask(perpetualTaskId, accountId);
      return;
    }
    if (EmptyPredicate.isEmpty(pollingDocument.getSignatures())) {
      pollingService.delete(pollingDocument);
      return;
    }
    if (executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      handleSuccessResponse(pollingDocument, executionResponse.getPollingResponseInfc());
    } else {
      handleFailureResponse(pollingDocument);
    }
  }

  private void handleSuccessResponse(PollingDocument pollingDocument, PollingResponseInfc pollingResponseInfc) {
    String accountId = pollingDocument.getAccountId();
    String pollDocId = pollingDocument.getUuid();

    if (pollingDocument.getFailedAttempts() > 0) {
      pollingService.updateFailedAttempts(accountId, pollDocId, 0);
    }

    List<String> newVersions = new ArrayList<>();

    switch (pollingDocument.getPollingType()) {
      case MANIFEST:
        handleManifestResponse(pollingDocument, pollingResponseInfc);
        break;
      case ARTIFACT:
        // TODO: Handle ArtifactReseponse
        if (isNotEmpty(newVersions)) {
          PolledResponseResult polledResponseResult =
              getPolledResponseResultForArtifact((ArtifactInfo) pollingDocument.getPollingInfo());
          publishPolledItemToTopic(pollingDocument, newVersions, polledResponseResult);
        }
        break;
      default:
        throw new InvalidRequestException(
            "Not implemented yet for " + pollingDocument.getPollingType() + " polling type");
    }
  }

  private void publishPolledItemToTopic(
      PollingDocument pollingDocument, List<String> newVersions, PolledResponseResult polledResponseResult) {
    polledItemPublisher.publishPolledItems(
        PollingResponse.newBuilder()
            .setAccountId(pollingDocument.getAccountId())
            .setBuildInfo(
                BuildInfo.newBuilder().setName(polledResponseResult.getName()).addAllVersions(newVersions).build())
            .setType(polledResponseResult.getType())
            .addAllSignatures(pollingDocument.getSignatures())
            .build());
  }

  private void handleFailureResponse(PollingDocument pollingDocument) {
    int failedCount = pollingDocument.getFailedAttempts() + 1;

    if (failedCount % 25 == 0) {
      pollingPerpetualTaskService.resetPerpetualTask(pollingDocument);
    }

    pollingService.updateFailedAttempts(pollingDocument.getAccountId(), pollingDocument.getUuid(), failedCount);

    if (failedCount == MAX_FAILED_ATTEMPTS) {
      pollingPerpetualTaskService.deletePerpetualTask(
          pollingDocument.getPerpetualTaskId(), pollingDocument.getAccountId());
    }
  }

  public void handleManifestResponse(PollingDocument pollingDocument, PollingResponseInfc pollingResponseInfc) {
    ManifestPollingDelegateResponse response = (ManifestPollingDelegateResponse) pollingResponseInfc;
    ManifestPolledResponse savedResponse = (ManifestPolledResponse) pollingDocument.getPolledResponse();
    String accountId = pollingDocument.getAccountId();
    String pollDocId = pollingDocument.getUuid();
    List<String> unpublishedManifests = response.getUnpublishedManifests();

    // If polled response is null, it means it was first time collecting output from perpetual task
    // There is no need to publish collected new versions in this case.
    if (savedResponse == null) {
      pollingService.updatePolledResponse(accountId, pollDocId,
          ManifestPolledResponse.builder().allPolledKeys(new HashSet<>(unpublishedManifests)).build());
      return;
    }

    // find if there are any new versions which are not in db. This is required because of delegate rebalancing,
    // delegate can loose context of latest versions.
    Set<String> savedManifests = savedResponse.getAllPolledKeys();
    List<String> newVersions = unpublishedManifests.stream()
                                   .filter(manifest -> !savedManifests.contains(manifest))
                                   .collect(Collectors.toList());

    if (isNotEmpty(newVersions)) {
      PolledResponseResult polledResponseResult =
          getPolledResponseResultForManifest((ManifestInfo) pollingDocument.getPollingInfo());
      publishPolledItemToTopic(pollingDocument, unpublishedManifests, polledResponseResult);
    }

    // after publishing event, update database as well.
    // if delegate rebalancing happened, unpublishedManifests are now the new versions. We might have to delete few keys
    // from db.
    Set<String> toBeDeletedKeys = response.getToBeDeletedKeys();
    Set<String> unpublishedManifestSet = new HashSet<>(unpublishedManifests);
    if (response.isFirstCollectionOnDelegate() && isEmpty(toBeDeletedKeys)) {
      toBeDeletedKeys = savedManifests.stream()
                            .filter(savedManifest -> !unpublishedManifestSet.contains(savedManifest))
                            .collect(Collectors.toSet());
    }
    savedManifests.removeAll(toBeDeletedKeys);
    savedManifests.addAll(unpublishedManifestSet);
    ManifestPolledResponse manifestPolledResponse =
        ManifestPolledResponse.builder().allPolledKeys(savedManifests).build();
    pollingService.updatePolledResponse(accountId, pollDocId, manifestPolledResponse);
  }

  private PolledResponseResult getPolledResponseResultForManifest(ManifestInfo manifestInfo) {
    PolledResponseResult polledResponseResult = PolledResponseResult.builder().build();
    if (manifestInfo.getType().equals(ManifestType.HelmChart)) {
      polledResponseResult.setName(((HelmChartManifestInfo) manifestInfo).getChartName());
      if (manifestInfo.getStore() instanceof HttpStoreConfig) {
        polledResponseResult.setType(HTTP_HELM);
      } else if (manifestInfo.getStore() instanceof S3StoreConfig) {
        polledResponseResult.setType(S3_HELM);
      } else if (manifestInfo.getStore() instanceof GcsStoreConfig) {
        polledResponseResult.setType(GCS_HELM);
      } else {
        throw new InvalidRequestException(String.format("Unsupported Manifest Type {} or manifest store {}",
            manifestInfo.getType(), manifestInfo.getStore().toString()));
      }
    }
    return polledResponseResult;
  }

  private PolledResponseResult getPolledResponseResultForArtifact(ArtifactInfo artifactInfo) {
    //    if (artifactConfig.getSourceType() == ArtifactSourceType.ECR) {
    //      polledResponseResult.setName(((EcrArtifactConfig) artifactConfig).getImagePath().getValue());
    //      polledResponseResult.setType(DOCKER_ECR);
    //    } else {
    //      throw new InvalidRequestException(String.format("Unsupported Artifact Type {}",
    //      artifactConfig.getSourceType()));
    //    }

    return null;
  }
}
