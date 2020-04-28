package software.wings.graphql.datafetcher.artifactSource;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.graphql.schema.type.artifactSource.QLACRArtifactSource;
import software.wings.graphql.schema.type.artifactSource.QLAMIArtifactSource;
import software.wings.graphql.schema.type.artifactSource.QLAmazonS3ArtifactSource;
import software.wings.graphql.schema.type.artifactSource.QLArtifactSource;
import software.wings.graphql.schema.type.artifactSource.QLArtifactoryArtifactSource;
import software.wings.graphql.schema.type.artifactSource.QLAzureArtifactsArtifactSource;
import software.wings.graphql.schema.type.artifactSource.QLBambooArtifactSource;
import software.wings.graphql.schema.type.artifactSource.QLCustomArtifactSource;
import software.wings.graphql.schema.type.artifactSource.QLDockerArtifactSource;
import software.wings.graphql.schema.type.artifactSource.QLECRArtifactSource;
import software.wings.graphql.schema.type.artifactSource.QLGCRArtifactSource;
import software.wings.graphql.schema.type.artifactSource.QLGCSArtifactSource;
import software.wings.graphql.schema.type.artifactSource.QLJenkinsArtifactSource;
import software.wings.graphql.schema.type.artifactSource.QLNexusArtifactSource;
import software.wings.graphql.schema.type.artifactSource.QLSFTPArtifactSource;
import software.wings.graphql.schema.type.artifactSource.QLSMBArtifactSource;

public class ArtifactSourceController {
  private ArtifactSourceController() {}

  public static QLArtifactSource populateArtifactSource(ArtifactStream artifactStream) {
    ArtifactStreamType artifactStreamType = ArtifactStreamType.valueOf(artifactStream.getArtifactStreamType());
    switch (artifactStreamType) {
      case ACR:
        return QLACRArtifactSource.builder()
            .id(artifactStream.getUuid())
            .name(artifactStream.getName())
            .createdAt(artifactStream.getCreatedAt())
            .build();
      case AMI:
        return QLAMIArtifactSource.builder()
            .id(artifactStream.getUuid())
            .name(artifactStream.getName())
            .createdAt(artifactStream.getCreatedAt())
            .build();

      case ECR:
        return QLECRArtifactSource.builder()
            .id(artifactStream.getUuid())
            .name(artifactStream.getName())
            .createdAt(artifactStream.getCreatedAt())
            .build();

      case GCR:
        return QLGCRArtifactSource.builder()
            .id(artifactStream.getUuid())
            .name(artifactStream.getName())
            .createdAt(artifactStream.getCreatedAt())
            .build();

      case GCS:
        return QLGCSArtifactSource.builder()
            .id(artifactStream.getUuid())
            .name(artifactStream.getName())
            .createdAt(artifactStream.getCreatedAt())
            .build();

      case SMB:
        return QLSMBArtifactSource.builder()
            .id(artifactStream.getUuid())
            .name(artifactStream.getName())
            .createdAt(artifactStream.getCreatedAt())
            .build();

      case SFTP:
        return QLSFTPArtifactSource.builder()
            .id(artifactStream.getUuid())
            .name(artifactStream.getName())
            .createdAt(artifactStream.getCreatedAt())
            .build();

      case NEXUS:
        return QLNexusArtifactSource.builder()
            .id(artifactStream.getUuid())
            .name(artifactStream.getName())
            .createdAt(artifactStream.getCreatedAt())
            .build();

      case BAMBOO:
        return QLBambooArtifactSource.builder()
            .id(artifactStream.getUuid())
            .name(artifactStream.getName())
            .createdAt(artifactStream.getCreatedAt())
            .build();

      case CUSTOM:
        return QLCustomArtifactSource.builder()
            .id(artifactStream.getUuid())
            .name(artifactStream.getName())
            .createdAt(artifactStream.getCreatedAt())
            .build();

      case DOCKER:
        return QLDockerArtifactSource.builder()
            .id(artifactStream.getUuid())
            .name(artifactStream.getName())
            .createdAt(artifactStream.getCreatedAt())
            .build();

      case JENKINS:
        return QLJenkinsArtifactSource.builder()
            .id(artifactStream.getUuid())
            .name(artifactStream.getName())
            .createdAt(artifactStream.getCreatedAt())
            .build();

      case AMAZON_S3:
        return QLAmazonS3ArtifactSource.builder()
            .id(artifactStream.getUuid())
            .name(artifactStream.getName())
            .createdAt(artifactStream.getCreatedAt())
            .build();

      case ARTIFACTORY:
        return QLArtifactoryArtifactSource.builder()
            .id(artifactStream.getUuid())
            .name(artifactStream.getName())
            .createdAt(artifactStream.getCreatedAt())
            .build();

      case AZURE_ARTIFACTS:
        return QLAzureArtifactsArtifactSource.builder()
            .id(artifactStream.getUuid())
            .name(artifactStream.getName())
            .createdAt(artifactStream.getCreatedAt())
            .build();
      default:
        throw new UnsupportedOperationException("Artifact stream type not supported: " + artifactStreamType);
    }
  }
}
