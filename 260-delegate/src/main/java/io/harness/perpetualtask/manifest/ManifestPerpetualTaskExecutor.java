package io.harness.perpetualtask.manifest;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.manifests.request.ManifestCollectionParams;
import io.harness.grpc.utils.AnyUtils;
import io.harness.managerclient.ManagerClient;
import io.harness.perpetualtask.PerpetualTaskExecutionParams;
import io.harness.perpetualtask.PerpetualTaskExecutor;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.artifact.ArtifactsPublishedCache;
import io.harness.serializer.KryoSerializer;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.appmanifest.HelmChart;

import java.time.Instant;

@Singleton
@Slf4j
@OwnedBy(CDC)
public class ManifestPerpetualTaskExecutor implements PerpetualTaskExecutor {
  private static final long INTERNAL_TIMEOUT_IN_MS = 90L * 1000;

  private final ManagerClient managerClient;
  private final KryoSerializer kryoSerializer;

  private final Cache<String, ArtifactsPublishedCache<HelmChart>> cache = Caffeine.newBuilder().build();

  @Inject
  public ManifestPerpetualTaskExecutor(ManagerClient managerClient, KryoSerializer kryoSerializer) {
    this.managerClient = managerClient;
    this.kryoSerializer = kryoSerializer;
  }

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    ManifestCollectionTaskParams manifestParams = getTaskParams(params);
    String appManifestId = manifestParams.getAppManifestId();
    logger.info("Started manifest collection for appManifestId:{}", appManifestId);
    ManifestCollectionParams manifestCollectionParams =
        (ManifestCollectionParams) kryoSerializer.asObject(manifestParams.getManifestCollectionParams().toByteArray());

    // TODO: Implement perpetual task collection, caching and sending back the response to manager

    logger.info("Published manifest successfully for app manifest: {}", manifestCollectionParams.getAppManifestId());
    return PerpetualTaskResponse.builder().responseCode(200).responseMessage("success").build();
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    ManifestCollectionTaskParams manifestParams = getTaskParams(params);
    cache.invalidate(manifestParams.getAppManifestId());
    return false;
  }

  private ManifestCollectionTaskParams getTaskParams(PerpetualTaskExecutionParams params) {
    return AnyUtils.unpack(params.getCustomizedParams(), ManifestCollectionTaskParams.class);
  }
}
