package io.harness.generator.artifactstream;

import static java.util.Arrays.asList;

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
  public ArtifactStream ensureArtifact(Seed seed, Owners owners) {
    Service service = owners.obtainService();

    Application application = owners.obtainApplication();

    final SettingAttribute settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, Settings.HARNESS_JENKINS_CONNECTOR);

    return ensureArtifactStream(seed,
        JenkinsArtifactStream.builder()
            .appId(application.getUuid())
            .serviceId(service.getUuid())
            .autoPopulate(false)
            .name("harness-samples")
            .sourceName(settingAttribute.getName())
            .jobname("harness-samples")
            .artifactPaths(asList("echo/target/echo.war"))
            .settingId(settingAttribute.getUuid())
            .build());
  }

  @Override
  public ArtifactStream ensureArtifactStream(Randomizer.Seed seed, ArtifactStream artifactStream) {
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

    return artifactStreamGeneratorHelper.saveArtifactSTream(builder.build());
  }
}
