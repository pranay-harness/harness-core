package software.wings.service.impl.applicationmanifest;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.Duration;
import com.google.protobuf.util.Durations;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.manifests.request.ManifestCollectionPTaskClientParams.ManifestCollectionPTaskClientParamsKeys;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.service.intfc.ApplicationManifestService;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class AppManifestPTaskHelper {
  public static final Duration ITERATION_INTERVAL = Durations.fromMinutes(1);
  public static final Duration TIMEOUT = Durations.fromMinutes(2);
  @Inject ApplicationManifestService applicationManifestService;
  @Inject PerpetualTaskService perpetualTaskService;

  public void createPerpetualTask(ApplicationManifest appManifest) {
    final String appManifestId = appManifest.getUuid();
    Validator.notNullCheck("Application manifest id is missing", appManifestId);
    if (appManifest.getPerpetualTaskId() != null) {
      throw new InvalidRequestException("A perpetual task is already assigned to this application manifest with id: "
          + appManifest.getPerpetualTaskId());
    }

    final String accountId = appManifest.getAccountId();
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Creating perpetual task for application manifest: {}", appManifestId);

      PerpetualTaskClientContext clientContext =
          PerpetualTaskClientContext.builder()
              .clientParams(ImmutableMap.of(ManifestCollectionPTaskClientParamsKeys.appManifestId, appManifestId,
                  ManifestCollectionPTaskClientParamsKeys.appId, appManifest.getAppId()))
              .build();
      PerpetualTaskSchedule schedule =
          PerpetualTaskSchedule.newBuilder().setInterval(ITERATION_INTERVAL).setTimeout(TIMEOUT).build();
      String perpetualTaskId = perpetualTaskService.createTask(PerpetualTaskType.MANIFEST_COLLECTION, accountId,
          clientContext, schedule, false, "Task to collect manifests for app manifest " + appManifestId);

      Validator.notNullCheck("Error in creating perpetual task for app manifest " + appManifestId, perpetualTaskId);
      if (!applicationManifestService.attachPerpetualTask(accountId, appManifestId, perpetualTaskId)) {
        logger.error("Unable to attach perpetual task {} for application manifest {}", perpetualTaskId, appManifestId);
        deletePerpetualTask(appManifest, perpetualTaskId);
      }

      logger.info("Perpetual task created successfully for application manifest: {}", appManifestId);
    } catch (Exception e) {
      logger.error("Unable to create perpetual task for app manifest: {}", appManifestId, e);
    }
  }

  public void deletePerpetualTask(ApplicationManifest appManifest, String perpetualTaskId) {
    if (!applicationManifestService.detachPerpetualTask(perpetualTaskId)) {
      logger.error(
          "Unable to detach perpetual task {} for application manifest {}", perpetualTaskId, appManifest.getUuid());
    }
    if (!perpetualTaskService.deleteTask(appManifest.getAccountId(), perpetualTaskId)) {
      logger.error(
          "Unable to delete perpetual task {} for application manifest {}", perpetualTaskId, appManifest.getUuid());
    }
    logger.info("Successfully deleted perpetual task {}", perpetualTaskId);
  }

  public void resetPerpetualTask(ApplicationManifest appManifest) {
    try (AutoLogContext ignore1 = new AccountLogContext(appManifest.getAccountId(), OVERRIDE_ERROR)) {
      if (!perpetualTaskService.resetTask(appManifest.getAccountId(), appManifest.getPerpetualTaskId(), null)) {
        logger.error("Unable to reset artifact collection perpetual task: {} for app manifest {}",
            appManifest.getPerpetualTaskId(), appManifest.getUuid());
      }
    }
  }
}
