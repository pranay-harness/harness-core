package io.harness.beans.steps.stepinfo;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.CIBeansTest;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.beans.steps.stepinfo.publish.artifact.Artifact;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactConnector;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;

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
    StepElement stepElement = YamlPipelineUtils.read(yamlString, StepElement.class);
    PublishStepInfo publishStepInfo = (PublishStepInfo) stepElement.getStepSpecType();

    TypeInfo nonYamlInfo = publishStepInfo.getNonYamlInfo();
    assertThat(nonYamlInfo.getStepInfoType()).isEqualTo(CIStepInfoType.PUBLISH);

    assertThat(publishStepInfo).isNotNull();
    assertThat(publishStepInfo.getIdentifier()).isEqualTo("publishArtifacts");
    assertThat(publishStepInfo.getNonYamlInfo().getStepInfoType()).isEqualTo(CIStepInfoType.PUBLISH);

    List<Artifact> artifacts = publishStepInfo.getPublishArtifacts();
    assertThat(artifacts).hasSize(7);

    List<Artifact.Type> types = artifacts.stream().map(Artifact::getType).collect(Collectors.toList());
    assertThat(types).containsExactlyInAnyOrder(Artifact.Type.FILE_PATTERN, Artifact.Type.FILE_PATTERN,
        Artifact.Type.FILE_PATTERN, Artifact.Type.DOCKER_FILE, Artifact.Type.DOCKER_FILE, Artifact.Type.DOCKER_FILE,
        Artifact.Type.DOCKER_IMAGE);

    List<ArtifactConnector.Type> connectorTypes =
        artifacts.stream().map(Artifact::getConnector).map(ArtifactConnector::getType).collect(Collectors.toList());

    assertThat(connectorTypes)
        .containsExactlyInAnyOrder(ArtifactConnector.Type.NEXUS, ArtifactConnector.Type.ARTIFACTORY,
            ArtifactConnector.Type.S3, ArtifactConnector.Type.ECR, ArtifactConnector.Type.DOCKERHUB,
            ArtifactConnector.Type.GCR, ArtifactConnector.Type.GCR);
  }
}
