package io.harness.generator.artifactstream;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.SettingGenerator;

import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.utils.RepositoryFormat;

public class Nexus3NpmArtifactStreamsGenerator extends NexusArtifactStreamsGenerator {
  @Override
  public ArtifactStream ensureArtifactStream(
      Randomizer.Seed seed, OwnerManager.Owners owners, boolean atConnector, boolean metadataOnly) {
    Service service = owners.obtainService();
    Application application = owners.obtainApplication();
    final SettingAttribute settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, SettingGenerator.Settings.HARNESS_NEXU3_CONNECTOR);
    return ensureArtifactStream(seed,
        NexusArtifactStream.builder()
            .appId(atConnector ? GLOBAL_APP_ID : application.getUuid())
            .serviceId(atConnector ? settingAttribute.getUuid() : getServiceId(service))
            .autoPopulate(false)
            .metadataOnly(metadataOnly)
            .name(metadataOnly ? "nexus3-npm-metadataOnly" : "nexus3-npm")
            .sourceName(settingAttribute.getName())
            .repositoryFormat(RepositoryFormat.npm.name())
            .jobname("npm-registry")
            .packageName("npm-app1")
            .settingId(settingAttribute.getUuid())
            .build(),
        owners);
  }
}
