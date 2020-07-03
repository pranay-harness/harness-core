package io.harness.beans.seriazlier;

import static io.harness.product.ci.addon.proto.AuthType.BASIC_AUTH;

import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.publish.artifact.Artifact;
import io.harness.beans.steps.stepinfo.publish.artifact.DockerFileArtifact;
import io.harness.beans.steps.stepinfo.publish.artifact.FilePatternArtifact;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactConnector;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactoryConnector;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.EcrConnector;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.GcrConnector;
import io.harness.product.ci.addon.proto.AuthType;
import io.harness.product.ci.addon.proto.BuildPublishImage;
import io.harness.product.ci.addon.proto.Connector;
import io.harness.product.ci.addon.proto.Destination;
import io.harness.product.ci.addon.proto.LocationType;
import io.harness.product.ci.addon.proto.UploadFile;
import io.harness.product.ci.engine.proto.PublishArtifactsStep;
import io.harness.product.ci.engine.proto.Step;
import org.apache.commons.codec.binary.Base64;

import java.util.Optional;

public class PublishStepProtobufSerializer implements ProtobufSerializer<PublishStepInfo> {
  public static final String TYPE_NOT_SUPPORTED = "%s not supported";
  public static final String TYPE_NOT_IMPLEMENTED_YET = "%s not implemented yet";

  @Override
  public String serialize(PublishStepInfo stepInfo) {
    return Base64.encodeBase64String(convertRestoreCacheStepInfo(stepInfo).toByteArray());
  }

  private Step convertRestoreCacheStepInfo(PublishStepInfo stepInfo) {
    PublishArtifactsStep.Builder publishArtifactsStepBuilder = PublishArtifactsStep.newBuilder();
    stepInfo.getPublishArtifacts().forEach(artifact -> {
      switch (artifact.getType()) {
        case FILE_PATTERN:
          publishArtifactsStepBuilder.addFiles(resolveFilePattern((FilePatternArtifact) artifact));
          break;
        case DOCKER_FILE:
          publishArtifactsStepBuilder.addImages(resolveDockerFile((DockerFileArtifact) artifact));
          break;
        case DOCKER_IMAGE:
          throw new IllegalArgumentException(String.format(TYPE_NOT_IMPLEMENTED_YET, artifact.getType()));
        default:
          break;
      }
    });
    return Step.newBuilder()
        .setId(stepInfo.getIdentifier())
        .setDisplayName(Optional.ofNullable(stepInfo.getDisplayName()).orElse(""))
        .setPublishArtifacts(publishArtifactsStepBuilder.build())
        .build();
  }

  private BuildPublishImage resolveDockerFile(DockerFileArtifact dockerFileArtifact) {
    BuildPublishImage.Builder imageBuilder = BuildPublishImage.newBuilder();
    imageBuilder.setContext(dockerFileArtifact.getContext());
    imageBuilder.setDockerFile(dockerFileArtifact.getDockerFile());
    imageBuilder.setDestination(Destination.newBuilder()
                                    .setConnector(getConnector(dockerFileArtifact))
                                    .setLocationType(getLocationType(dockerFileArtifact.getConnector().getType()))
                                    .setDestinationUrl(getImageDestinationUrl(dockerFileArtifact.getConnector(),
                                        dockerFileArtifact.getImage(), dockerFileArtifact.getTag()))
                                    .build());
    return imageBuilder.build();
  }

  private UploadFile resolveFilePattern(FilePatternArtifact filePatternArtifact) {
    UploadFile.Builder uploadFileBuilder = UploadFile.newBuilder();
    uploadFileBuilder.setFilePattern(filePatternArtifact.getFilePattern());
    uploadFileBuilder.setDestination(Destination.newBuilder()
                                         .setConnector(getConnector(filePatternArtifact))
                                         .setLocationType(getLocationType(filePatternArtifact.getConnector().getType()))
                                         .setDestinationUrl(getFileDestinationUrl(filePatternArtifact.getConnector()))
                                         .build());
    return uploadFileBuilder.build();
  }

  private Connector getConnector(Artifact artifact) {
    return Connector.newBuilder()
        .setAuth(getAuthType(artifact.getConnector().getType()))
        .setId(artifact.getConnector().getConnector())
        .build();
  }

  private String getFileDestinationUrl(ArtifactConnector connector) {
    switch (connector.getType()) {
      case ARTIFACTORY:
        ArtifactoryConnector artifactoryConnector = (ArtifactoryConnector) connector;
        return artifactoryConnector.getRepository() + artifactoryConnector.getArtifactPath();
      case NEXUS:
      case S3:
        throw new IllegalArgumentException(String.format(TYPE_NOT_IMPLEMENTED_YET, connector.getType()));
      default:
        throw new IllegalArgumentException(String.format(TYPE_NOT_SUPPORTED, connector.getType()));
    }
  }
  private String getImageDestinationUrl(ArtifactConnector connector, String image, String tag) {
    switch (connector.getType()) {
      case ECR:
        EcrConnector ecrConnector = (EcrConnector) connector;
        return ecrConnector.getLocation();
      case GCR:
        GcrConnector gcrConnector = (GcrConnector) connector;
        return gcrConnector.getLocation();
      case ARTIFACTORY:
        ArtifactoryConnector artifactoryConnector = (ArtifactoryConnector) connector;
        return artifactoryConnector.getRepository() + artifactoryConnector.getArtifactPath();
      case DOCKERHUB:
        return String.format("%s:%s", image, tag);
      case NEXUS:
      case S3:
        throw new IllegalArgumentException(String.format(TYPE_NOT_IMPLEMENTED_YET, connector.getType()));
      default:
        throw new IllegalArgumentException(String.format(TYPE_NOT_SUPPORTED, connector.getType()));
    }
  }

  private LocationType getLocationType(ArtifactConnector.Type type) {
    switch (type) {
      case ECR:
        return LocationType.ECR;
      case GCR:
        return LocationType.GCR;
      case ARTIFACTORY:
        return LocationType.JFROG;
      case DOCKERHUB:
        return LocationType.DOCKERHUB;
      case S3:
      case NEXUS:
        throw new IllegalArgumentException(String.format(TYPE_NOT_IMPLEMENTED_YET, type));
      default:
        throw new IllegalArgumentException(String.format(TYPE_NOT_SUPPORTED, type));
    }
  }

  private AuthType getAuthType(ArtifactConnector.Type type) {
    switch (type) {
      case ECR:
        return AuthType.ACCESS_KEY;
      case GCR:
        return AuthType.SECRET_FILE;
      case ARTIFACTORY:
      case DOCKERHUB:
        return BASIC_AUTH;
      case NEXUS:
      case S3:
        throw new IllegalArgumentException(String.format(TYPE_NOT_IMPLEMENTED_YET, type));
      default:
        throw new IllegalArgumentException(String.format(TYPE_NOT_SUPPORTED, type));
    }
  }
}
