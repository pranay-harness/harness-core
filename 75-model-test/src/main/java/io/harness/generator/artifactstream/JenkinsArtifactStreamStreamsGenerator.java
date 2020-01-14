package io.harness.generator.artifactstream;

import static java.util.Arrays.asList;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SettingGenerator;
import io.harness.generator.SettingGenerator.Settings;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream.JenkinsArtifactStreamBuilder;

@Singleton
public class JenkinsArtifactStreamStreamsGenerator implements ArtifactStreamsGenerator {
  @Inject private SettingGenerator settingGenerator;
  @Inject private ArtifactStreamGeneratorHelper artifactStreamGeneratorHelper;

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners) {
    return ensureArtifactStream(seed, owners, false);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners, boolean atConnector) {
    return ensureArtifactStream(seed, owners, atConnector, false);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Seed seed, Owners owners, boolean atConnector, boolean metadataOnly) {
    Service service = owners.obtainService();
    Application application = owners.obtainApplication();
    final SettingAttribute settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, Settings.HARNESS_JENKINS_CONNECTOR_CD_TEAM);
    return ensureArtifactStream(seed,
        JenkinsArtifactStream.builder()
            .appId(atConnector ? GLOBAL_APP_ID : application.getUuid())
            .serviceId(atConnector ? settingAttribute.getUuid() : service != null ? service.getUuid() : null)
            .autoPopulate(false)
            .metadataOnly(metadataOnly)
            .name(metadataOnly ? "jenkins-harness-samples-metadataOnly" : "jenkins-harness-samples")
            .sourceName(settingAttribute.getName())
            .jobname("harness-samples")
            .artifactPaths(asList("echo/target/echo.war"))
            .settingId(settingAttribute.getUuid())
            .build(),
        owners);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Randomizer.Seed seed, ArtifactStream artifactStream, Owners owners) {
    JenkinsArtifactStream jenkinsArtifactStream = (JenkinsArtifactStream) artifactStream;
    final JenkinsArtifactStreamBuilder builder = JenkinsArtifactStream.builder();

    builder.appId(artifactStream.getAppId());

    builder.serviceId(artifactStream.getServiceId());

    builder.name(artifactStream.getName());

    ArtifactStream existing = artifactStreamGeneratorHelper.exists(builder.build());
    if (existing != null) {
      return existing;
    }
    if (jenkinsArtifactStream.getJobname() != null) {
      builder.jobname(jenkinsArtifactStream.getJobname());
    } else {
      throw new UnsupportedOperationException();
    }

    if (jenkinsArtifactStream.getArtifactPaths() != null) {
      builder.artifactPaths(jenkinsArtifactStream.getArtifactPaths());
    } else {
      throw new UnsupportedOperationException();
    }

    if (artifactStream.getSourceName() != null) {
      builder.sourceName(artifactStream.getSourceName());
    } else {
      throw new UnsupportedOperationException();
    }

    if (artifactStream.getSettingId() != null) {
      builder.settingId(artifactStream.getSettingId());
    } else {
      throw new UnsupportedOperationException();
    }

    builder.metadataOnly(artifactStream.isMetadataOnly());

    return artifactStreamGeneratorHelper.saveArtifactStream(builder.build(), owners);
  }
}
