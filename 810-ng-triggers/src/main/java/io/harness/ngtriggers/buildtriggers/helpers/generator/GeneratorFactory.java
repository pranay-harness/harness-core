package io.harness.ngtriggers.buildtriggers.helpers.generator;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ngtriggers.beans.source.ManifestType.HELM_MANIFEST;
import static io.harness.ngtriggers.beans.source.NGTriggerType.ARTIFACT;
import static io.harness.ngtriggers.beans.source.NGTriggerType.MANIFEST;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.source.artifact.BuildStoreType;
import io.harness.ngtriggers.buildtriggers.helpers.BuildTriggerHelper;
import io.harness.ngtriggers.buildtriggers.helpers.dtos.BuildTriggerOpsData;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(PIPELINE)
public class GeneratorFactory {
  private final BuildTriggerHelper buildTriggerHelper;
  private final HttpHelmPollingItemGenerator httpHelmPollingItemGenerator;
  private final S3HelmPollingItemGenerator s3HelmPollingItemGenerator;
  private final GCSHelmPollingItemGenerator gcsHelmPollingItemGenerator;

  public PollingItemGenerator retrievePollingItemGenerator(BuildTriggerOpsData buildTriggerOpsData) {
    NGTriggerEntity ngTriggerEntity = buildTriggerOpsData.getTriggerDetails().getNgTriggerEntity();
    if (ngTriggerEntity.getType() == MANIFEST) {
      return retrievePollingItemGeneratorForManifest(buildTriggerOpsData);
    } else if (ngTriggerEntity.getType() == ARTIFACT) {
      return retrievePollingItemGeneratorForArtifact(buildTriggerOpsData);
    }

    return null;
  }

  private PollingItemGenerator retrievePollingItemGeneratorForArtifact(BuildTriggerOpsData buildTriggerOpsData) {
    return null;
  }

  private PollingItemGenerator retrievePollingItemGeneratorForManifest(BuildTriggerOpsData buildTriggerOpsData) {
    String buildType = buildTriggerHelper.fetchBuildType(buildTriggerOpsData.getTriggerSpecMap());
    if (HELM_MANIFEST.getValue().equals(buildType)) {
      return retrievePollingItemGeneratorForHelmChart(buildTriggerOpsData);
    }

    return null;
  }

  private PollingItemGenerator retrievePollingItemGeneratorForHelmChart(BuildTriggerOpsData buildTriggerOpsData) {
    String storeTypeFromTrigger = buildTriggerHelper.fetchStoreTypeForHelm(buildTriggerOpsData);
    if (BuildStoreType.HTTP.getValue().equals(storeTypeFromTrigger)) {
      return httpHelmPollingItemGenerator;
    } else if (BuildStoreType.S3.getValue().equals(storeTypeFromTrigger)) {
      return s3HelmPollingItemGenerator;
    } else if (BuildStoreType.GCS.getValue().equals(storeTypeFromTrigger)) {
      return gcsHelmPollingItemGenerator;
    }

    return null;
  }
}
