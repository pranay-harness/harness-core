package io.harness.beans.serializer;

import static io.harness.product.ci.engine.proto.AuthType.BASIC_AUTH;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.publish.artifact.Artifact;
import io.harness.beans.steps.stepinfo.publish.artifact.DockerFileArtifact;
import io.harness.beans.steps.stepinfo.publish.artifact.FilePatternArtifact;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactConnector;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactoryConnector;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.EcrConnector;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.GcrConnector;
import io.harness.callback.DelegateCallbackToken;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.product.ci.engine.proto.AuthType;
import io.harness.product.ci.engine.proto.BuildPublishImage;
import io.harness.product.ci.engine.proto.Connector;
import io.harness.product.ci.engine.proto.Destination;
import io.harness.product.ci.engine.proto.LocationType;
import io.harness.product.ci.engine.proto.PublishArtifactsStep;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.product.ci.engine.proto.UploadFile;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import java.util.function.Supplier;

@Singleton
public class PublishStepProtobufSerializer implements ProtobufStepSerializer<PublishStepInfo> {
  public static final String TYPE_NOT_SUPPORTED = "%s not supported";
  public static final String TYPE_NOT_IMPLEMENTED_YET = "%s not implemented yet";
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  public UnitStep serializeStep(StepElementConfig step, Integer port, String callbackId) {
    CIStepInfo ciStepInfo = (CIStepInfo) step.getStepSpecType();
    PublishStepInfo publishStepInfo = (PublishStepInfo) ciStepInfo;

    if (callbackId == null) {
      throw new CIStageExecutionException("CallbackId can not be null");
    }

    PublishArtifactsStep.Builder publishArtifactsStepBuilder = PublishArtifactsStep.newBuilder();
    publishStepInfo.getPublishArtifacts().forEach(artifact -> {
      switch (artifact.getType()) {
        case FILE_PATTERN:
          publishArtifactsStepBuilder.addFiles(
              resolveFilePattern((FilePatternArtifact) artifact, step.getIdentifier()));
          break;
        case DOCKER_FILE:
          publishArtifactsStepBuilder.addImages(resolveDockerFile((DockerFileArtifact) artifact, step.getIdentifier()));
          break;
        case DOCKER_IMAGE:
          throw new IllegalArgumentException(String.format(TYPE_NOT_IMPLEMENTED_YET, artifact.getType()));
        default:
          break;
      }
    });

    String skipCondition = SkipConditionUtils.getSkipCondition(step);
    return UnitStep.newBuilder()
        .setId(step.getIdentifier())
        .setTaskId(callbackId)
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(Optional.ofNullable(step.getName()).orElse(""))
        .setPublishArtifacts(publishArtifactsStepBuilder.build())
        .setSkipCondition(Optional.ofNullable(skipCondition).orElse(""))
        .build();
  }

  private BuildPublishImage resolveDockerFile(DockerFileArtifact dockerFileArtifact, String identifier) {
    BuildPublishImage.Builder imageBuilder = BuildPublishImage.newBuilder();
    String context = RunTimeInputHandler.resolveStringParameter(
        "context", "DockerHub", identifier, dockerFileArtifact.getContext(), false);
    if (context != null && !context.equals(RunTimeInputHandler.UNRESOLVED_PARAMETER)) {
      imageBuilder.setContext(context);
    }

    String dockerFile = RunTimeInputHandler.resolveStringParameter(
        "dockerfile", "DockerHub", identifier, dockerFileArtifact.getDockerFile(), false);
    if (dockerFile != null && !dockerFile.equals(RunTimeInputHandler.UNRESOLVED_PARAMETER)) {
      imageBuilder.setDockerFile(dockerFile);
    }

    imageBuilder.setDestination(Destination.newBuilder()
                                    .setConnector(getConnector(dockerFileArtifact, identifier))
                                    .setLocationType(getLocationType(dockerFileArtifact.getConnector().getType()))
                                    .setDestinationUrl(getImageDestinationUrl(dockerFileArtifact.getConnector(),
                                        RunTimeInputHandler.resolveStringParameter(
                                            "Image", "DockerHub", identifier, dockerFileArtifact.getImage(), true),
                                        RunTimeInputHandler.resolveStringParameter(
                                            "Tag", "DockerHub", identifier, dockerFileArtifact.getTag(), true),
                                        identifier))
                                    .build());
    return imageBuilder.build();
  }

  private UploadFile resolveFilePattern(FilePatternArtifact filePatternArtifact, String identifier) {
    UploadFile.Builder uploadFileBuilder = UploadFile.newBuilder();
    uploadFileBuilder.setFilePattern(filePatternArtifact.getFilePattern());
    uploadFileBuilder.setDestination(
        Destination.newBuilder()
            .setConnector(getConnector(filePatternArtifact, identifier))
            .setLocationType(getLocationType(filePatternArtifact.getConnector().getType()))
            .setDestinationUrl(getFileDestinationUrl(filePatternArtifact.getConnector(), identifier))
            .build());
    return uploadFileBuilder.build();
  }

  private Connector getConnector(Artifact artifact, String identifier) {
    return Connector.newBuilder()
        .setAuth(getAuthType(artifact.getConnector().getType()))
        .setId(IdentifierRefHelper.getIdentifier(RunTimeInputHandler.resolveStringParameter("ConnectorRef",
            artifact.getConnector().getType().name(), identifier, artifact.getConnector().getConnectorRef(), true)))
        .build();
  }

  private String getFileDestinationUrl(ArtifactConnector connector, String identifier) {
    switch (connector.getType()) {
      case ARTIFACTORY:
        ArtifactoryConnector artifactoryConnector = (ArtifactoryConnector) connector;
        return RunTimeInputHandler.resolveStringParameter(
                   "Repository", "ARTIFACTORY", identifier, artifactoryConnector.getRepository(), true)
            + RunTimeInputHandler.resolveStringParameter(
                "ArtifactPath", "ARTIFACTORY", identifier, artifactoryConnector.getArtifactPath(), true);
      case NEXUS:
      case S3:
        throw new IllegalArgumentException(String.format(TYPE_NOT_IMPLEMENTED_YET, connector.getType()));
      default:
        throw new IllegalArgumentException(String.format(TYPE_NOT_SUPPORTED, connector.getType()));
    }
  }
  private String getImageDestinationUrl(ArtifactConnector connector, String image, String tag, String identifier) {
    switch (connector.getType()) {
      case ECR:
        EcrConnector ecrConnector = (EcrConnector) connector;
        return RunTimeInputHandler.resolveStringParameter(
            "Location", "ECR", identifier, ecrConnector.getLocation(), true);
      case GCR:
        GcrConnector gcrConnector = (GcrConnector) connector;
        return RunTimeInputHandler.resolveStringParameter(
            "Location", "GCR", identifier, gcrConnector.getLocation(), true);
      case ARTIFACTORY:
        ArtifactoryConnector artifactoryConnector = (ArtifactoryConnector) connector;
        return RunTimeInputHandler.resolveStringParameter(
                   "Repository", "ARTIFACTORY", identifier, artifactoryConnector.getRepository(), true)
            + RunTimeInputHandler.resolveStringParameter(
                "ArtifactPath", "ARTIFACTORY", identifier, artifactoryConnector.getArtifactPath(), true);
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
