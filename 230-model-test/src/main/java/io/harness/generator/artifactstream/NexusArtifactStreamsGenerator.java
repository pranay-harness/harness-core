package io.harness.generator.artifactstream;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.SettingGenerator;

import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream.NexusArtifactStreamBuilder;
import software.wings.utils.RepositoryFormat;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.CDC)
public abstract class NexusArtifactStreamsGenerator implements ArtifactStreamsGenerator {
  @Inject protected SettingGenerator settingGenerator;
  @Inject private ArtifactStreamGeneratorHelper artifactStreamGeneratorHelper;

  @Override
  public ArtifactStream ensureArtifactStream(Randomizer.Seed seed, OwnerManager.Owners owners, boolean atConnector) {
    return ensureArtifactStream(seed, owners, atConnector, false);
  }

  @Override
  public ArtifactStream ensureArtifactStream(Randomizer.Seed seed, OwnerManager.Owners owners) {
    return ensureArtifactStream(seed, owners, false);
  }

  protected String getServiceId(Service service) {
    return service != null ? service.getUuid() : null;
  }

  @Override
  public ArtifactStream ensureArtifactStream(
      Randomizer.Seed seed, ArtifactStream artifactStream, OwnerManager.Owners owners) {
    NexusArtifactStream nexusArtifactStream = (NexusArtifactStream) artifactStream;
    final NexusArtifactStreamBuilder builder = NexusArtifactStream.builder();

    builder.appId(artifactStream.getAppId());

    builder.serviceId(artifactStream.getServiceId());

    builder.name(artifactStream.getName());

    ArtifactStream existing = artifactStreamGeneratorHelper.exists(builder.build());
    if (existing != null) {
      return existing;
    }

    String repositoryFormat = nexusArtifactStream.getRepositoryFormat();
    if (repositoryFormat.equals(RepositoryFormat.maven.name())) {
      Preconditions.checkNotNull(nexusArtifactStream.getJobname());
      Preconditions.checkNotNull(nexusArtifactStream.getGroupId());
      Preconditions.checkNotNull(nexusArtifactStream.getArtifactPaths());
      builder.jobname(nexusArtifactStream.getJobname());
      builder.groupId(nexusArtifactStream.getGroupId());
      builder.artifactPaths(nexusArtifactStream.getArtifactPaths());
      builder.repositoryFormat(repositoryFormat);
    } else if (repositoryFormat.equals(RepositoryFormat.docker.name())) {
      Preconditions.checkNotNull(nexusArtifactStream.getJobname());
      Preconditions.checkNotNull(nexusArtifactStream.getImageName());
      builder.jobname(nexusArtifactStream.getJobname());
      builder.imageName(nexusArtifactStream.getPackageName());
      builder.dockerRegistryUrl(nexusArtifactStream.getPackageName());
      builder.repositoryFormat(repositoryFormat);
    } else if (repositoryFormat.equals(RepositoryFormat.npm.name())
        || repositoryFormat.equals(RepositoryFormat.nuget.name())) {
      Preconditions.checkNotNull(nexusArtifactStream.getJobname());
      Preconditions.checkNotNull(nexusArtifactStream.getPackageName());
      builder.jobname(nexusArtifactStream.getJobname());
      builder.packageName(nexusArtifactStream.getPackageName());
      builder.repositoryFormat(repositoryFormat);
    }

    Preconditions.checkNotNull(nexusArtifactStream.getSourceName());
    Preconditions.checkNotNull(nexusArtifactStream.getSettingId());
    builder.sourceName(artifactStream.getSourceName());
    builder.settingId(artifactStream.getSettingId());
    builder.metadataOnly(artifactStream.isMetadataOnly());

    return artifactStreamGeneratorHelper.saveArtifactStream(builder.build(), owners);
  }
}
