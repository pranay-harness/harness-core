package software.wings.service.impl.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.perpetualtask.PerpetualTaskService;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.FeatureName;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.delegatetasks.buildsource.ArtifactStreamLogContext;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.artifact.ArtifactStreamServiceObserver;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class ArtifactStreamPTaskManager implements ArtifactStreamServiceObserver {
  @Inject private ArtifactStreamPTaskHelper artifactStreamPTaskHelper;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private PerpetualTaskService perpetualTaskService;

  @Override
  public void onSaved(ArtifactStream artifactStream) {
    if (featureFlagService.isEnabled(FeatureName.ARTIFACT_PERPETUAL_TASK, artifactStream.getAccountId())) {
      artifactStreamPTaskHelper.createPerpetualTask(artifactStream);
    }
  }

  @Override
  public void onUpdated(ArtifactStream currArtifactStream) {
    if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_PERPETUAL_TASK, currArtifactStream.getAccountId())) {
      return;
    }

    if (currArtifactStream.getPerpetualTaskId() == null) {
      artifactStreamPTaskHelper.createPerpetualTask(currArtifactStream);
      return;
    }

    try (AutoLogContext ignore1 = new AccountLogContext(currArtifactStream.getAccountId(), OVERRIDE_ERROR);
         AutoLogContext ignore2 = new ArtifactStreamLogContext(currArtifactStream.getUuid(), OVERRIDE_ERROR)) {
      if (!perpetualTaskService.resetTask(
              currArtifactStream.getAccountId(), currArtifactStream.getPerpetualTaskId(), null)) {
        logger.error(
            format("Unable to reset artifact collection perpetual task: %s", currArtifactStream.getPerpetualTaskId()));
      }
    }
  }

  @Override
  public void onDeleted(ArtifactStream artifactStream) {
    if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_PERPETUAL_TASK, artifactStream.getAccountId())) {
      return;
    }

    if (artifactStream.getPerpetualTaskId() != null) {
      try (AutoLogContext ignore1 = new AccountLogContext(artifactStream.getAccountId(), OVERRIDE_ERROR);
           AutoLogContext ignore2 = new ArtifactStreamLogContext(artifactStream.getUuid(), OVERRIDE_ERROR)) {
        if (!perpetualTaskService.deleteTask(artifactStream.getAccountId(), artifactStream.getPerpetualTaskId())) {
          logger.error(
              format("Unable to delete artifact collection perpetual task: %s", artifactStream.getPerpetualTaskId()));
        }
      }
    }
  }
}
