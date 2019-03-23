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
import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.artifact.AmazonS3ArtifactStream.AmazonS3ArtifactStreamBuilder;
import software.wings.beans.artifact.ArtifactStream;

@Singleton
public class AmazonS3ArtifactStreamStreamsGenerator implements ArtifactStreamsGenerator {
  @Inject private SettingGenerator settingGenerator;
  @Inject private ArtifactStreamGeneratorHelper artifactStreamGeneratorHelper;

  @Override
  public ArtifactStream ensureArtifact(Seed seed, Owners owners) {
    Service service = owners.obtainService();
    Application application = owners.obtainApplication();

    final SettingAttribute settingAttribute =
        settingGenerator.ensurePredefined(seed, owners, Settings.AWS_TEST_CLOUD_PROVIDER);

    return ensureArtifactStream(seed,
        AmazonS3ArtifactStream.builder()
            .appId(application.getUuid())
            .serviceId(service.getUuid())
            .name("harness-iis-app")
            .sourceName(settingAttribute.getName())
            .jobname("iis-app-example")
            .artifactPaths(asList("todolist-v2.0.zip"))
            .settingId(settingAttribute.getUuid())
            .build());
  }

  @Override
  public ArtifactStream ensureArtifactStream(Randomizer.Seed seed, ArtifactStream artifactStream) {
    AmazonS3ArtifactStream amazonS3ArtifactStream = (AmazonS3ArtifactStream) artifactStream;
    final AmazonS3ArtifactStreamBuilder s3ArtifactStreamBuilder = AmazonS3ArtifactStream.builder();

    if (artifactStream != null && artifactStream.getAppId() != null) {
      s3ArtifactStreamBuilder.appId(artifactStream.getAppId());
    } else {
      throw new UnsupportedOperationException();
    }

    if (artifactStream.getServiceId() != null) {
      s3ArtifactStreamBuilder.serviceId(artifactStream.getServiceId());
    } else {
      throw new UnsupportedOperationException();
    }

    if (artifactStream.getName() != null) {
      s3ArtifactStreamBuilder.name(artifactStream.getName());
    } else {
      throw new UnsupportedOperationException();
    }

    ArtifactStream existingArtifactStream = artifactStreamGeneratorHelper.exists(s3ArtifactStreamBuilder.build());
    if (existingArtifactStream != null) {
      return existingArtifactStream;
    }

    if (amazonS3ArtifactStream.getJobname() != null) {
      s3ArtifactStreamBuilder.jobname(amazonS3ArtifactStream.getJobname());
    } else {
      throw new UnsupportedOperationException();
    }

    if (amazonS3ArtifactStream.getArtifactPaths() != null) {
      s3ArtifactStreamBuilder.artifactPaths(amazonS3ArtifactStream.getArtifactPaths());
    } else {
      throw new UnsupportedOperationException();
    }

    if (artifactStream.getSourceName() != null) {
      s3ArtifactStreamBuilder.sourceName(artifactStream.getSourceName());
    } else {
      throw new UnsupportedOperationException();
    }

    if (artifactStream.getSettingId() != null) {
      s3ArtifactStreamBuilder.settingId(artifactStream.getSettingId());
    } else {
      throw new UnsupportedOperationException();
    }

    return artifactStreamGeneratorHelper.saveArtifactStream(s3ArtifactStreamBuilder.build());
  }
}
