package software.wings.service.impl.artifact;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.FeatureName;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.logcontext.SettingAttributeLogContext;
import software.wings.service.impl.SettingAttributeObserver;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FeatureFlagService;

import java.util.List;

@Slf4j
@Singleton
public class ArtifactStreamSettingAttributePTaskManager implements SettingAttributeObserver {
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ArtifactStreamPTaskHelper artifactStreamPTaskHelper;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public void onSaved(SettingAttribute settingAttribute) {
    // Nothing to do on save.
  }

  @Override
  public void onUpdated(SettingAttribute prevSettingAttribute, SettingAttribute currSettingAttribute) {
    if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_PERPETUAL_TASK, currSettingAttribute.getAccountId())) {
      return;
    }

    try (AutoLogContext ignore1 = new AccountLogContext(currSettingAttribute.getAccountId(), OVERRIDE_ERROR);
         AutoLogContext ignore2 = new SettingAttributeLogContext(currSettingAttribute.getUuid(), OVERRIDE_ERROR)) {
      List<ArtifactStream> artifactStreams = artifactStreamService.listAllBySettingId(currSettingAttribute.getUuid());
      if (isEmpty(artifactStreams)) {
        return;
      }

      artifactStreams.forEach(this ::resetPerpetualTask);
    }
  }

  @Override
  public void onDeleted(SettingAttribute settingAttribute) {
    // Nothing to do on delete as artifact streams are themselves deleted.
  }

  private void resetPerpetualTask(ArtifactStream artifactStream) {
    if (artifactStream.getPerpetualTaskId() != null) {
      if (!artifactStreamPTaskHelper.reset(artifactStream.getAccountId(), artifactStream.getPerpetualTaskId())) {
        logger.error(
            format("Unable to reset artifact collection perpetual task: %s", artifactStream.getPerpetualTaskId()));
      }
    }
  }
}
