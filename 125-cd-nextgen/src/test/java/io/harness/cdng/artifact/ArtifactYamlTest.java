package io.harness.cdng.artifact;

import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifact;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifactWrapper;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.cdng.service.beans.KubernetesServiceSpec;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.rule.Owner;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.utils.YamlPipelineUtils;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDC)
public class ArtifactYamlTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  @Ignore("Referring to old Pojos, which needs to be deleted and updated.")
  public void testParseArtifactYamlForDocker() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("cdng/artifact.yml");
    NgPipeline ngPipeline = YamlPipelineUtils.read(testFile, NgPipeline.class);

    assertThat(ngPipeline.getIdentifier()).isEqualTo("prod_primary_deployment");
    assertThat(ngPipeline.getStages().size()).isEqualTo(2);

    // First Stage
    StageElementWrapper stageWrapper = ngPipeline.getStages().get(0);
    assertThat(stageWrapper).isInstanceOf(StageElement.class);
    assertThat(((StageElement) stageWrapper).getStageType()).isInstanceOf(DeploymentStage.class);
    DeploymentStage deploymentStage = (DeploymentStage) ((StageElement) stageWrapper).getStageType();
    KubernetesServiceSpec serviceSpec =
        (KubernetesServiceSpec) deploymentStage.getServiceConfig().getServiceDefinition().getServiceSpec();
    assertThat(serviceSpec).isNotNull();

    assertThat(serviceSpec.getArtifacts().getPrimary()).isNotNull();
    assertThat(serviceSpec.getArtifacts().getSidecars()).isNotNull();
    assertThat(serviceSpec.getArtifacts().getSidecars().size()).isEqualTo(2);

    ArtifactConfig primary = serviceSpec.getArtifacts().getPrimary().getSpec();
    assertThat(primary).isInstanceOf(DockerHubArtifactConfig.class);
    DockerHubArtifactConfig dockerArtifact = (DockerHubArtifactConfig) primary;
    assertThat(dockerArtifact.getImagePath().getValue()).isEqualTo("library/ubuntu");
    assertThat(dockerArtifact.getTag().getValue()).isEqualTo("latest");
    assertThat(dockerArtifact.getTagRegex().getValue()).isEqualTo("groov*");
    assertThat(dockerArtifact.isPrimaryArtifact()).isTrue();
    assertThat(dockerArtifact.getUniqueHash())
        .isEqualTo(ArtifactUtils.generateUniqueHashFromStringList(
            Arrays.asList(dockerArtifact.getConnectorRef().getValue(), dockerArtifact.getImagePath().getValue())));

    SidecarArtifactWrapper sidecarArtifactWrapper = serviceSpec.getArtifacts().getSidecars().get(0);
    SidecarArtifact sidecarArtifact = sidecarArtifactWrapper.getSidecar();
    assertThat(sidecarArtifact.getSpec()).isInstanceOf(DockerHubArtifactConfig.class);
    assertThat(sidecarArtifact.getIdentifier()).isEqualTo("sidecar1");
    dockerArtifact = (DockerHubArtifactConfig) sidecarArtifact.getSpec();
    assertThat(dockerArtifact.getIdentifier()).isEqualTo("sidecar1");
    assertThat(dockerArtifact.getImagePath().getValue()).isEqualTo("library/redis");
    assertThat(dockerArtifact.getTag().getValue()).isEqualTo("latest");
    assertThat(dockerArtifact.isPrimaryArtifact()).isFalse();
  }
}
