package io.harness.beans.serializer;

import static io.harness.product.ci.engine.proto.AuthType.ACCESS_KEY;
import static io.harness.product.ci.engine.proto.AuthType.BASIC_AUTH;
import static io.harness.product.ci.engine.proto.AuthType.SECRET_FILE;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;

import io.harness.beans.CIBeansTest;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.publish.artifact.DockerFileArtifact;
import io.harness.beans.steps.stepinfo.publish.artifact.FilePatternArtifact;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactoryConnector;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.DockerhubConnector;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.EcrConnector;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.GcrConnector;
import io.harness.category.element.UnitTests;
import io.harness.product.ci.engine.proto.BuildPublishImage;
import io.harness.product.ci.engine.proto.LocationType;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.product.ci.engine.proto.UploadFile;
import io.harness.rule.Owner;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PublishStepProtobufSerializerTest extends CIBeansTest {
  public static final String PUBLISH_ID = "publish-id";
  public static final String PUBLISH_NAME = "publish-name";
  public static final String FILE_PATTERN = "~/dir/.*";
  public static final String DOCKER_HUB_CONNECTOR = "docker-hub-connector";
  public static final String DOCKER_FILE = "~/dockerFile";
  public static final String CONTEXT = "~/workspace";
  public static final String IMAGE = "image";
  public static final String TAG = "tag";
  public static final String ARTIFACT_PATH = "<artifact-path>";
  public static final String REPOSITORY = "http://<host>:<port>/artifactory/<remote-repository-name>/";
  public static final String ARTIFACTORY_CONNECTOR = "artifactory-connector";
  public static final String KEY = "key";
  public static final String VALUE = "value";
  public static final String ECR_CONNECTOR = "ecr-connector";
  public static final String ECR_REGION = "us-west";
  public static final String ECR_LOCATION = "aws_account_id.dkr.ecr.region.amazonaws.com/my-web-app";
  public static final String GCR_CONNECTOR = "gcr-connector";
  public static final String GCR_LOCATION = "us.gcr.io/project/image";
  public static final String CALLBACK_ID = "callbackId";
  @Inject ProtobufSerializer<PublishStepInfo> protobufSerializer;

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldSerializeFilePatternArtifact() throws InvalidProtocolBufferException {
    FilePatternArtifact filePatternArtifact = FilePatternArtifact.builder()
                                                  .filePattern(FILE_PATTERN)
                                                  .connector(ArtifactoryConnector.builder()
                                                                 .connectorRef(ARTIFACTORY_CONNECTOR)
                                                                 .artifactPath(ARTIFACT_PATH)
                                                                 .repository(REPOSITORY)
                                                                 .build())
                                                  .build();

    PublishStepInfo publishStepInfo = PublishStepInfo.builder()
                                          .name(PUBLISH_NAME)
                                          .identifier(PUBLISH_ID)
                                          .publishArtifacts(singletonList(filePatternArtifact))
                                          .build();
    publishStepInfo.setCallbackId(CALLBACK_ID);
    String serialize = protobufSerializer.serialize(publishStepInfo);
    UnitStep publishArtifactStep = UnitStep.parseFrom(Base64.decodeBase64(serialize));
    assertThat(publishArtifactStep.getId()).isEqualTo(PUBLISH_ID);
    assertThat(publishArtifactStep.getDisplayName()).isEqualTo(PUBLISH_NAME);
    UploadFile uploadFile = publishArtifactStep.getPublishArtifacts().getFiles(0);
    assertThat(uploadFile.getFilePattern()).isEqualTo(FILE_PATTERN);
    assertThat(uploadFile.getDestination().getDestinationUrl()).isEqualTo(REPOSITORY + ARTIFACT_PATH);
    assertThat(uploadFile.getDestination().getConnector().getId()).isEqualTo(ARTIFACTORY_CONNECTOR);
    assertThat(uploadFile.getDestination().getConnector().getAuth()).isEqualTo(BASIC_AUTH);
    assertThat(uploadFile.getDestination().getLocationType()).isEqualTo(LocationType.JFROG);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldSerializeDockerFileArtifactOnGcrConnector() throws InvalidProtocolBufferException {
    DockerFileArtifact dockerFileArtifact =
        DockerFileArtifact.builder()
            .buildArguments(singletonList(DockerFileArtifact.BuildArgument.builder().key(KEY).value(VALUE).build()))
            .connector(GcrConnector.builder().connectorRef(GCR_CONNECTOR).location(GCR_LOCATION).build())
            .dockerFile(DOCKER_FILE)
            .context(CONTEXT)
            .image(IMAGE)
            .tag(TAG)
            .build();

    PublishStepInfo publishStepInfo = PublishStepInfo.builder()
                                          .name(PUBLISH_NAME)
                                          .identifier(PUBLISH_ID)
                                          .callbackId(CALLBACK_ID)
                                          .publishArtifacts(singletonList(dockerFileArtifact))
                                          .build();

    String serialize = protobufSerializer.serialize(publishStepInfo);
    UnitStep publishArtifactStep = UnitStep.parseFrom(Base64.decodeBase64(serialize));
    assertThat(publishArtifactStep.getId()).isEqualTo(PUBLISH_ID);
    assertThat(publishArtifactStep.getDisplayName()).isEqualTo(PUBLISH_NAME);
    BuildPublishImage buildPublishImage = publishArtifactStep.getPublishArtifacts().getImages(0);
    assertThat(buildPublishImage.getContext()).isEqualTo(CONTEXT);
    assertThat(buildPublishImage.getDockerFile()).isEqualTo(DOCKER_FILE);
    assertThat(buildPublishImage.getDestination().getDestinationUrl()).isEqualTo(GCR_LOCATION);
    assertThat(buildPublishImage.getDestination().getConnector().getId()).isEqualTo(GCR_CONNECTOR);
    assertThat(buildPublishImage.getDestination().getConnector().getAuth()).isEqualTo(SECRET_FILE);
    assertThat(buildPublishImage.getDestination().getLocationType()).isEqualTo(LocationType.GCR);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldSerializeDockerFileArtifactOnDockerHubConnector() throws InvalidProtocolBufferException {
    DockerFileArtifact dockerFileArtifact =
        DockerFileArtifact.builder()
            .buildArguments(singletonList(DockerFileArtifact.BuildArgument.builder().key(KEY).value(VALUE).build()))
            .connector(DockerhubConnector.builder().connectorRef(DOCKER_HUB_CONNECTOR).build())
            .dockerFile(DOCKER_FILE)
            .context(CONTEXT)
            .image(IMAGE)
            .tag(TAG)
            .build();

    PublishStepInfo publishStepInfo = PublishStepInfo.builder()
                                          .name(PUBLISH_NAME)
                                          .callbackId(CALLBACK_ID)
                                          .identifier(PUBLISH_ID)
                                          .publishArtifacts(singletonList(dockerFileArtifact))
                                          .build();

    String serialize = protobufSerializer.serialize(publishStepInfo);
    UnitStep publishArtifactStep = UnitStep.parseFrom(Base64.decodeBase64(serialize));
    assertThat(publishArtifactStep.getId()).isEqualTo(PUBLISH_ID);
    assertThat(publishArtifactStep.getDisplayName()).isEqualTo(PUBLISH_NAME);
    BuildPublishImage buildPublishImage = publishArtifactStep.getPublishArtifacts().getImages(0);
    assertThat(buildPublishImage.getContext()).isEqualTo(CONTEXT);
    assertThat(buildPublishImage.getDockerFile()).isEqualTo(DOCKER_FILE);
    assertThat(buildPublishImage.getDestination().getDestinationUrl()).isEqualTo(IMAGE + ":" + TAG);
    assertThat(buildPublishImage.getDestination().getConnector().getId()).isEqualTo(DOCKER_HUB_CONNECTOR);
    assertThat(buildPublishImage.getDestination().getConnector().getAuth()).isEqualTo(BASIC_AUTH);
    assertThat(buildPublishImage.getDestination().getLocationType()).isEqualTo(LocationType.DOCKERHUB);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldSerializeDockerFileArtifactOnEcrConnector() throws InvalidProtocolBufferException {
    DockerFileArtifact dockerFileArtifact =
        DockerFileArtifact.builder()
            .buildArguments(singletonList(DockerFileArtifact.BuildArgument.builder().key(KEY).value(VALUE).build()))
            .connector(
                EcrConnector.builder().connectorRef(ECR_CONNECTOR).location(ECR_LOCATION).region(ECR_REGION).build())
            .dockerFile(DOCKER_FILE)
            .context(CONTEXT)
            .image(IMAGE)
            .tag(TAG)
            .build();

    PublishStepInfo publishStepInfo = PublishStepInfo.builder()
                                          .name(PUBLISH_NAME)
                                          .identifier(PUBLISH_ID)
                                          .callbackId(CALLBACK_ID)
                                          .publishArtifacts(singletonList(dockerFileArtifact))
                                          .build();

    String serialize = protobufSerializer.serialize(publishStepInfo);
    UnitStep publishArtifactStep = UnitStep.parseFrom(Base64.decodeBase64(serialize));
    assertThat(publishArtifactStep.getId()).isEqualTo(PUBLISH_ID);
    assertThat(publishArtifactStep.getDisplayName()).isEqualTo(PUBLISH_NAME);
    BuildPublishImage buildPublishImage = publishArtifactStep.getPublishArtifacts().getImages(0);
    assertThat(buildPublishImage.getContext()).isEqualTo(CONTEXT);
    assertThat(buildPublishImage.getDockerFile()).isEqualTo(DOCKER_FILE);
    assertThat(buildPublishImage.getDestination().getDestinationUrl()).isEqualTo(ECR_LOCATION);
    assertThat(buildPublishImage.getDestination().getConnector().getId()).isEqualTo(ECR_CONNECTOR);
    assertThat(buildPublishImage.getDestination().getConnector().getAuth()).isEqualTo(ACCESS_KEY);
    assertThat(buildPublishImage.getDestination().getLocationType()).isEqualTo(LocationType.ECR);
  }
}