package io.harness.beans.steps.stepinfo;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.CIBeansTest;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.beans.steps.stepinfo.publish.artifact.Artifact;
import io.harness.beans.steps.stepinfo.publish.artifact.ArtifactType;
import io.harness.beans.steps.stepinfo.publish.artifact.DockerFileArtifact;
import io.harness.beans.steps.stepinfo.publish.artifact.DockerImageArtifact;
import io.harness.beans.steps.stepinfo.publish.artifact.FilePatternArtifact;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class PublishStepInfoTest extends CIBeansTest {
  private String yamlString;
  @Before
  public void setUp() {
    yamlString = new Scanner(
        Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("publish_step.yml")), "UTF-8")
                     .useDelimiter("\\A")
                     .next();
  }
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testPublishStepConversion() throws IOException {
    PublishStepInfo publishStepInfo = YamlPipelineUtils.read(yamlString, PublishStepInfo.class);

    TypeInfo nonYamlInfo = publishStepInfo.getNonYamlInfo();
    assertThat(nonYamlInfo.getStepInfoType()).isEqualTo(CIStepInfoType.PUBLISH);

    assertThat(publishStepInfo).isNotNull();
    assertThat(publishStepInfo.getIdentifier()).isEqualTo("publishArtifacts");
    assertThat(publishStepInfo.getNonYamlInfo().getStepInfoType()).isEqualTo(CIStepInfoType.PUBLISH);

    List<Artifact> artifacts = publishStepInfo.getPublishArtifacts();
    assertThat(artifacts).hasSize(6);

    assertThat(artifacts.get(0).getType()).isEqualTo(ArtifactType.FILE_PATTERN);
    FilePatternArtifact filePattern1 = (FilePatternArtifact) artifacts.get(0);
    assertThat(filePattern1.getFilePattern()).isEqualTo("~/.m2/*-${BUILD_NUMBER}.jar");
    assertThat(filePattern1.getDestination().getLocation()).isEqualTo("gs://snapshot-delegate-jars/artifacts/");
    assertThat(filePattern1.getDestination().getConnector()).isEqualTo("myGCPConnector");

    assertThat(artifacts.get(1).getType()).isEqualTo(ArtifactType.FILE_PATTERN);

    assertThat(artifacts.get(2).getType()).isEqualTo(ArtifactType.DOCKER_FILE);
    DockerFileArtifact dockerFileArtifact1 = (DockerFileArtifact) artifacts.get(2);
    assertThat(dockerFileArtifact1.getDockerFile()).isEqualTo("~/Dockerfile");
    assertThat(dockerFileArtifact1.getImage()).isEqualTo("ui");
    assertThat(dockerFileArtifact1.getTag()).isEqualTo("1.0.0");
    assertThat(dockerFileArtifact1.getBuildArguments().get(0).get("key")).isEqualTo("value");
    assertThat(dockerFileArtifact1.getDestination().getLocation()).isEqualTo("eu.gcr.io/harness/ui:latest");
    assertThat(dockerFileArtifact1.getDestination().getConnector()).isEqualTo("myDockerRepoConnector");

    assertThat(artifacts.get(3).getType()).isEqualTo(ArtifactType.DOCKER_IMAGE);
    DockerImageArtifact dockerImageArtifact1 = (DockerImageArtifact) artifacts.get(3);
    assertThat(dockerImageArtifact1.getDockerImage()).isEqualTo("build-based-image");
    assertThat(dockerImageArtifact1.getTag()).isEqualTo("1.0.0");
    assertThat(dockerImageArtifact1.getDestination()).isNull();

    assertThat(artifacts.get(4).getType()).isEqualTo(ArtifactType.DOCKER_IMAGE);
    DockerImageArtifact dockerImageArtifact2 = (DockerImageArtifact) artifacts.get(4);
    assertThat(dockerImageArtifact2.getDockerImage()).isEqualTo("build-based-image");
    assertThat(dockerImageArtifact2.getTag()).isEqualTo("1.0.0");
    assertThat(dockerImageArtifact2.getDestination().getConnector()).isEqualTo("myDockerHubConnector");
    assertThat(dockerImageArtifact2.getDestination().getLocation()).isNull();

    assertThat(artifacts.get(5).getType()).isEqualTo(ArtifactType.DOCKER_IMAGE);
    DockerImageArtifact dockerImageArtifact3 = (DockerImageArtifact) artifacts.get(5);

    assertThat(dockerImageArtifact3.getDockerImage()).isEqualTo("build-based-image");
    assertThat(dockerImageArtifact3.getTag()).isEqualTo("1.0.0");
    assertThat(dockerImageArtifact3.getDestination().getConnector()).isEqualTo("myGcrConnector");
    assertThat(dockerImageArtifact3.getDestination().getLocation()).isEqualTo("gs://myBucket/ciApp");
  }
}