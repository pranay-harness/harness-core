package io.harness.polling;

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
import io.harness.delegate.beans.polling.ManifestPollingResponseInfc;
import io.harness.delegate.beans.polling.PollingResponseInfc;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.polling.bean.PolledResponse;
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

import software.wings.service.impl.PollingDelegateResponse;

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
  public PollingResponseHandler(
      PollingService pollingService, PollingPerpetualTaskService pollingPerpetualTaskService) {
    this.pollingService = pollingService;
    this.pollingPerpetualTaskService = pollingPerpetualTaskService;
  }

  public void handlePollingResponse(
      @NotEmpty String perpetualTaskId, @NotEmpty String accountId, PollingDelegateResponse executionResponse) {
    String pollDocId = executionResponse.getPollingDocId();
    PollingDocument pollingDocument = pollingService.get(accountId, pollDocId);
    if (pollingDocument == null || !perpetualTaskId.equals(pollingDocument.getPerpetualTaskId())) {
      pollingPerpetualTaskService.deletePerpetualTask(perpetualTaskId, accountId);
      return;
    }
    if (EmptyPredicate.isEmpty(pollingDocument.getSignature())) {
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
    pollingService.updateFailedAttempts(accountId, pollDocId, 0);

    List<String> newVersions = new ArrayList<>();

    switch (pollingDocument.getPollingType()) {
      case MANIFEST:
        PolledResponse polledResponse = pollingDocument.getPolledResponse();
        newVersions = handleManifestResponse(accountId, pollDocId, polledResponse, pollingResponseInfc);
        if (isNotEmpty(newVersions)) {
          PolledResponseResult polledResponseResult =
              getPolledResponseResultForManifest((ManifestInfo) pollingDocument.getPollingInfo());
          publishPolledItemToQueue(pollingDocument, newVersions, polledResponseResult);
        }
        break;
      case ARTIFACT:
        // TODO: Handle ArtifactReseponse
        if (isNotEmpty(newVersions)) {
          PolledResponseResult polledResponseResult =
              getPolledResponseResultForArtifact((ArtifactInfo) pollingDocument.getPollingInfo());
          publishPolledItemToQueue(pollingDocument, newVersions, polledResponseResult);
        }
        break;
      default:
        throw new InvalidRequestException(
            "Not implemented yet for " + pollingDocument.getPollingType() + " polling type");
    }
  }

  private void publishPolledItemToQueue(
      PollingDocument pollingDocument, List<String> newVersions, PolledResponseResult polledResponseResult) {
    polledItemPublisher.publishPolledItems(
        PollingResponse.newBuilder()
            .setAccountId(pollingDocument.getAccountId())
            .setBuildInfo(
                BuildInfo.newBuilder().setName(polledResponseResult.getName()).addAllVersions(newVersions).build())
            .setType(polledResponseResult.getType())
            .addAllSignatures(pollingDocument.getSignature())
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

  public List<String> handleManifestResponse(
      String accountId, String pollDocId, PolledResponse polledResponse, PollingResponseInfc pollingResponseInfc) {
    ManifestPollingResponseInfc response = (ManifestPollingResponseInfc) pollingResponseInfc;
    ManifestPolledResponse savedResponse = (ManifestPolledResponse) polledResponse;
    List<String> allVersions = response.getAllVersions();
    List<String> newVersions = response.getUnpublishedVersions();

    // If polled response is null, it means it was first time collecting output from perpetual task
    // There is no need to publish collected new versions in this case.
    if (savedResponse == null) {
      pollingService.updatePolledResponse(
          accountId, pollDocId, ManifestPolledResponse.builder().allPolledKeys(new HashSet<>(newVersions)).build());
      return null;
    }

    // find if there are any new versions which are not in db. This is required because of delegate rebalancing,
    // delegate might have lose context of latest versions.
    Set<String> savedVersions = savedResponse.getAllPolledKeys();
    newVersions = newVersions.stream().filter(version -> !savedVersions.contains(version)).collect(Collectors.toList());

    ManifestPolledResponse manifestPolledResponse =
        ManifestPolledResponse.builder().allPolledKeys(new HashSet<>(allVersions)).build();
    pollingService.updatePolledResponse(accountId, pollDocId, manifestPolledResponse);
    return newVersions;
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
