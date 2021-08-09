package io.harness.perpetualtask.polling.manifest;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.network.SafeHttpCall.executeWithExceptions;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.polling.ManifestPollingResponseInfc;
import io.harness.delegate.task.k8s.ManifestDelegateConfig;
import io.harness.grpc.utils.AnyUtils;
import io.harness.logging.CommandExecutionStatus;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.perpetualtask.PerpetualTaskExecutionParams;
import io.harness.perpetualtask.PerpetualTaskExecutor;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.polling.ManifestCollectionTaskParamsNg;
import io.harness.serializer.KryoSerializer;

import software.wings.service.impl.PollingDelegateResponse;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;

@Slf4j
@OwnedBy(HarnessTeam.CDC)
public class ManifestPerpetualTaskExecutorNg implements PerpetualTaskExecutor {
  private final KryoSerializer kryoSerializer;
  private final ManifestCollectionService manifestCollectionService;
  private final DelegateAgentManagerClient delegateAgentManagerClient;

  private final @Getter Cache<String, ManifestsCollectionCache> cache = Caffeine.newBuilder().build();
  private static final long TIMEOUT_IN_MILLIS = 100L * 1000;

  @Inject
  public ManifestPerpetualTaskExecutorNg(KryoSerializer kryoSerializer,
      ManifestCollectionService manifestCollectionService, DelegateAgentManagerClient delegateAgentManagerClient) {
    this.kryoSerializer = kryoSerializer;
    this.manifestCollectionService = manifestCollectionService;
    this.delegateAgentManagerClient = delegateAgentManagerClient;
  }

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    ManifestCollectionTaskParamsNg taskParams = getTaskParams(params);
    String pollingDocId = taskParams.getPollingDocId();
    String perpetualTaskId = taskId.getId();
    ManifestsCollectionCache manifestsCollectionCache = cache.get(pollingDocId, id -> new ManifestsCollectionCache());

    Instant startTime = Instant.now();
    if (!manifestsCollectionCache.needsToPublish()) {
      collectManifests(manifestsCollectionCache, taskParams, perpetualTaskId);
    }

    if (manifestsCollectionCache.needsToPublish()) {
      Instant deadline = startTime.plusMillis(TIMEOUT_IN_MILLIS);
      publishFromCache(manifestsCollectionCache, deadline, taskParams, perpetualTaskId);
    }

    return PerpetualTaskResponse.builder().responseCode(200).responseMessage("success").build();
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    ManifestCollectionTaskParamsNg taskParams = getTaskParams(params);
    cache.invalidate(taskParams.getPollingDocId());
    ManifestDelegateConfig manifestConfig =
        (ManifestDelegateConfig) kryoSerializer.asObject(taskParams.getManifestCollectionParams().toByteArray());
    manifestCollectionService.cleanup(manifestConfig);
    return false;
  }

  private ManifestCollectionTaskParamsNg getTaskParams(PerpetualTaskExecutionParams params) {
    return AnyUtils.unpack(params.getCustomizedParams(), ManifestCollectionTaskParamsNg.class);
  }

  private void collectManifests(
      ManifestsCollectionCache manifestsCollectionCache, ManifestCollectionTaskParamsNg taskParams, String taskId) {
    try {
      ManifestDelegateConfig manifestConfig =
          (ManifestDelegateConfig) kryoSerializer.asObject(taskParams.getManifestCollectionParams().toByteArray());
      List<String> chartVersions = manifestCollectionService.collectManifests(manifestConfig);
      if (isEmpty(chartVersions)) {
        log.info("No manifests present for the repository");
        return;
      }
      manifestsCollectionCache.populateCache(chartVersions);
    } catch (Exception e) {
      log.error("Error while collecting manifests ", e);
      publishToManger(taskId,
          PollingDelegateResponse.builder()
              .accountId(taskParams.getAccountId())
              .commandExecutionStatus(CommandExecutionStatus.FAILURE)
              .errorMessage(e.getMessage())
              .pollingDocId(taskParams.getPollingDocId())
              .build());
    }
  }

  private boolean publishToManger(String taskId, PollingDelegateResponse pollingDelegateResponse) {
    try {
      byte[] responseSerialized = kryoSerializer.asBytes(pollingDelegateResponse);

      executeWithExceptions(
          delegateAgentManagerClient.publishPollingResult(taskId, pollingDelegateResponse.getAccountId(),
              RequestBody.create(MediaType.parse("application/octet-stream"), responseSerialized)));
      return true;
    } catch (Exception ex) {
      log.error("Failed to publish manifest polling response with status: {}",
          pollingDelegateResponse.getCommandExecutionStatus().name(), ex);
      return false;
    }
  }

  private void publishFromCache(ManifestsCollectionCache manifestsCollectionCache, Instant expiryTime,
      ManifestCollectionTaskParamsNg taskParams, String taskId) {
    if (expiryTime.isBefore(Instant.now())) {
      log.warn("Manifest Collection timed out after {} seconds",
          Instant.now().compareTo(expiryTime.minusMillis(TIMEOUT_IN_MILLIS)));
      return;
    }

    List<String> unpublishedKeys = new ArrayList<>(manifestsCollectionCache.getUnpublishedManifestKeys());
    if (isEmpty(unpublishedKeys)) {
      return;
    }

    // TODO: check if need to do reverse sort as doing reverse sort can be incorrect in case of strings.
    unpublishedKeys.sort(Collections.reverseOrder());

    PollingDelegateResponse response =
        PollingDelegateResponse.builder()
            .accountId(taskParams.getAccountId())
            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
            .pollingDocId(taskParams.getPollingDocId())
            .pollingResponseInfc(ManifestPollingResponseInfc.builder()
                                     .unpublishedVersions(unpublishedKeys)
                                     .allVersions(new ArrayList<>(manifestsCollectionCache.getAllManifestKeys()))
                                     .build())
            .build();

    if (publishToManger(taskId, response)) {
      manifestsCollectionCache.clearUnpublishedVersions(unpublishedKeys);
    }
  }
}
