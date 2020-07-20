package io.harness.cdng.artifact;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.ArtifactSourceType;
import io.harness.cdng.artifact.bean.SidecarArtifactWrapper;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSource;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifact;
import io.harness.cdng.artifact.delegate.DockerArtifactServiceImpl;
import io.harness.cdng.artifact.delegate.beans.ArtifactSourceAttributes;
import io.harness.cdng.artifact.delegate.beans.DockerArtifactSourceAttributes;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.cdng.service.beans.KubernetesServiceSpec;
import io.harness.rule.Owner;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.utils.WingsTestConstants;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

public class ArtifactYamlTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testParseArtifactYamlForDocker() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("cdng/artifact.yml");
    CDPipeline cdPipeline = YamlPipelineUtils.read(testFile, CDPipeline.class);

    assertThat(cdPipeline.getIdentifier()).isEqualTo("prod_primary_deployment");
    assertThat(cdPipeline.getStages().size()).isEqualTo(2);

    // First Stage
    StageElementWrapper stageWrapper = cdPipeline.getStages().get(0);
    assertThat(stageWrapper).isInstanceOf(StageElement.class);
    assertThat(((StageElement) stageWrapper).getStageType()).isInstanceOf(DeploymentStage.class);
    DeploymentStage deploymentStage = (DeploymentStage) ((StageElement) stageWrapper).getStageType();
    KubernetesServiceSpec serviceSpec =
        (KubernetesServiceSpec) deploymentStage.getService().getServiceDef().getServiceSpec();
    assertThat(serviceSpec).isNotNull();

    assertThat(serviceSpec.getArtifacts().getPrimary()).isNotNull();
    assertThat(serviceSpec.getArtifacts().getSidecars()).isNotNull();
    assertThat(serviceSpec.getArtifacts().getSidecars().size()).isEqualTo(2);

    ArtifactConfig primary = serviceSpec.getArtifacts().getPrimary().getArtifactConfig();
    assertThat(primary).isInstanceOf(DockerHubArtifactConfig.class);
    DockerHubArtifactConfig dockerArtifact = (DockerHubArtifactConfig) primary;
    assertThat(dockerArtifact.getImagePath()).isEqualTo("library/ubuntu");
    assertThat(dockerArtifact.getTag()).isEqualTo("latest");
    assertThat(dockerArtifact.getTagRegex()).isEqualTo("groov*");
    assertThat(dockerArtifact.getArtifactType()).isEqualTo(ArtifactUtils.PRIMARY_ARTIFACT);

    SidecarArtifactWrapper sidecarArtifactWrapper = serviceSpec.getArtifacts().getSidecars().get(0);
    assertThat(sidecarArtifactWrapper).isInstanceOf(SidecarArtifact.class);
    SidecarArtifact sidecarArtifact = (SidecarArtifact) sidecarArtifactWrapper;
    assertThat(sidecarArtifact.getArtifactConfig()).isInstanceOf(DockerHubArtifactConfig.class);
    assertThat(sidecarArtifact.getIdentifier()).isEqualTo("sidecar1");
    dockerArtifact = (DockerHubArtifactConfig) sidecarArtifact.getArtifactConfig();
    assertThat(dockerArtifact.getIdentifier()).isEqualTo("sidecar1");
    assertThat(dockerArtifact.getImagePath()).isEqualTo("library/redis");
    assertThat(dockerArtifact.getTag()).isEqualTo("latest");
    assertThat(dockerArtifact.getArtifactType()).isEqualTo(ArtifactUtils.SIDECAR_ARTIFACT);

    // DockerArtifactSource and SourceAttributes test
    ArtifactSource artifactSource = dockerArtifact.getArtifactSource(WingsTestConstants.ACCOUNT_ID);
    assertThat(artifactSource).isInstanceOf(DockerArtifactSource.class);
    DockerArtifactSource dockerArtifactSource = (DockerArtifactSource) artifactSource;
    assertThat(dockerArtifactSource.getImagePath()).isEqualTo("library/redis");
    assertThat(dockerArtifactSource.getUniqueHash())
        .isEqualTo(ArtifactUtils.generateUniqueHashFromStringList(
            Arrays.asList(dockerArtifact.getConnectorIdentifier(), dockerArtifact.getImagePath())));
    assertThat(dockerArtifactSource.getSourceType()).isEqualTo(ArtifactSourceType.DOCKER_HUB);

    ArtifactSourceAttributes sourceAttributes = dockerArtifact.getSourceAttributes();
    assertThat(sourceAttributes).isInstanceOf(DockerArtifactSourceAttributes.class);
    DockerArtifactSourceAttributes dockerArtifactSourceAttributes = (DockerArtifactSourceAttributes) sourceAttributes;
    assertThat(dockerArtifactSourceAttributes.getDelegateArtifactServiceClass())
        .isEqualTo(DockerArtifactServiceImpl.class);
    assertThat(dockerArtifactSourceAttributes.getImagePath()).isEqualTo(dockerArtifact.getImagePath());
    assertThat(dockerArtifactSourceAttributes.getDockerhubConnector())
        .isEqualTo(dockerArtifact.getConnectorIdentifier());
    assertThat(dockerArtifactSourceAttributes.getTag()).isEqualTo(dockerArtifact.getTag());
  }
}