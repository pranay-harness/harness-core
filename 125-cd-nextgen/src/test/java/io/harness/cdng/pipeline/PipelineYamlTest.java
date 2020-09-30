package io.harness.cdng.pipeline;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.InputSetValidatorType;
import io.harness.beans.ParameterField;
import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.SidecarArtifactWrapper;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifact;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.cdng.k8s.K8sRollingRollbackStepInfo;
import io.harness.cdng.k8s.K8sRollingStepInfo;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.ManifestConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.service.beans.KubernetesServiceSpec;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.StageOverridesConfig;
import io.harness.rule.Owner;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class PipelineYamlTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testPipelineWithRuntimeInputYaml() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("cdng/pipelineWithRuntimeInput.yml");
    NgPipeline ngPipeline = YamlPipelineUtils.read(testFile, NgPipeline.class);
    assertThat(ngPipeline.getIdentifier()).isEqualTo("myPipeline1");
    assertThat(ngPipeline.getStages().size()).isEqualTo(2);

    // First Stage
    StageElementWrapper stageWrapper = ngPipeline.getStages().get(0);
    DeploymentStage deploymentStage = (DeploymentStage) ((StageElement) stageWrapper).getStageType();

    // Service
    ServiceConfig service = deploymentStage.getService();
    KubernetesServiceSpec serviceSpec = (KubernetesServiceSpec) service.getServiceDefinition().getServiceSpec();
    assertThat(serviceSpec).isNotNull();
    assertThat(service.getIdentifier()).isInstanceOf(ParameterField.class);
    assertThat(service.getIdentifier().isExpression()).isTrue();
    assertThat(service.getIdentifier().getExpressionValue()).isEqualTo("${input}");

    // Primary Artifacts
    ArtifactConfig primary = serviceSpec.getArtifacts().getPrimary().getArtifactConfig();
    assertThat(primary).isInstanceOf(DockerHubArtifactConfig.class);
    DockerHubArtifactConfig dockerArtifact = (DockerHubArtifactConfig) primary;
    assertThat(dockerArtifact.getImagePath()).isInstanceOf(ParameterField.class);
    assertThat(dockerArtifact.getImagePath().isExpression()).isTrue();
    assertThat(dockerArtifact.getImagePath().getExpressionValue()).isEqualTo("${input}");

    // Sidecar Artifact
    SidecarArtifactWrapper sidecarArtifactWrapper = serviceSpec.getArtifacts().getSidecars().get(0);
    assertThat(sidecarArtifactWrapper).isInstanceOf(SidecarArtifact.class);
    SidecarArtifact sidecarArtifact = (SidecarArtifact) sidecarArtifactWrapper;
    assertThat(sidecarArtifact.getArtifactConfig()).isInstanceOf(DockerHubArtifactConfig.class);
    dockerArtifact = (DockerHubArtifactConfig) sidecarArtifact.getArtifactConfig();
    assertThat(dockerArtifact.getTag()).isInstanceOf(ParameterField.class);
    assertThat(dockerArtifact.getTag().isExpression()).isTrue();
    assertThat(dockerArtifact.getTag().getExpressionValue()).isEqualTo("${input}");
    assertThat(dockerArtifact.getTag().getInputSetValidator()).isNull();

    // Manifests
    ManifestConfigWrapper manifestConfigWrapper = serviceSpec.getManifests().get(0);
    assertThat(manifestConfigWrapper).isInstanceOf(ManifestConfig.class);
    ManifestConfig manifestConfig = (ManifestConfig) manifestConfigWrapper;
    GitStore storeConfig = (GitStore) manifestConfig.getManifestAttributes().getStoreConfig();
    assertThat(storeConfig.getPaths()).isInstanceOf(ParameterField.class);
    assertThat(storeConfig.getPaths().isExpression()).isTrue();
    assertThat(storeConfig.getPaths().getExpressionValue()).isEqualTo("${input}");
    assertThat(storeConfig.getPaths().getInputSetValidator()).isNotNull();
    assertThat(storeConfig.getPaths().getInputSetValidator().getValidatorType())
        .isEqualTo(InputSetValidatorType.ALLOWED_VALUES);
    assertThat(storeConfig.getPaths().getInputSetValidator().getParameters())
        .isEqualTo("['paths1', 'master/paths2', 'paths3']");

    // manifestOverrideSet
    manifestConfigWrapper = serviceSpec.getManifestOverrideSets().get(0).getManifests().get(0);
    manifestConfig = (ManifestConfig) manifestConfigWrapper;
    storeConfig = (GitStore) manifestConfig.getManifestAttributes().getStoreConfig();
    assertThat(storeConfig.getConnectorIdentifier()).isInstanceOf(ParameterField.class);
    assertThat(storeConfig.getConnectorIdentifier().isExpression()).isTrue();
    assertThat(storeConfig.getConnectorIdentifier().getExpressionValue()).isEqualTo("${input}");
    assertThat(storeConfig.getPaths().getInputSetValidator()).isNull();

    // Infrastructure & environment
    PipelineInfrastructure infrastructure = deploymentStage.getInfrastructure();
    EnvironmentYaml environment = infrastructure.getEnvironment();
    assertThat(environment.getIdentifier()).isInstanceOf(ParameterField.class);
    assertThat(environment.getIdentifier().isExpression()).isTrue();
    assertThat(environment.getIdentifier().getExpressionValue()).isEqualTo("${input}");
    assertThat(environment.getName()).isInstanceOf(ParameterField.class);
    assertThat(environment.getName().isExpression()).isTrue();
    assertThat(environment.getName().getExpressionValue()).isEqualTo("${input}");
    K8SDirectInfrastructure infraDefinition =
        (K8SDirectInfrastructure) infrastructure.getInfrastructureDefinition().getInfrastructure();
    assertThat(infraDefinition.getNamespace()).isInstanceOf(ParameterField.class);
    assertThat(infraDefinition.getNamespace().isExpression()).isTrue();
    assertThat(infraDefinition.getNamespace().getExpressionValue()).isEqualTo("${input}");
    assertThat(infraDefinition.getNamespace().getInputSetValidator()).isNotNull();
    assertThat(infraDefinition.getNamespace().getInputSetValidator().getValidatorType())
        .isEqualTo(InputSetValidatorType.ALLOWED_VALUES);
    assertThat(infraDefinition.getNamespace().getInputSetValidator().getParameters())
        .isEqualTo("jexl(${stage.name} == 'qa' ? 'dev1, qa1':'prod1, stage1')");

    // Execution, Steps and Rollback
    List<ExecutionWrapper> steps = deploymentStage.getExecution().getSteps();
    ParallelStepElement parallelStepElement = (ParallelStepElement) steps.get(0);
    StepElement stepElement = (StepElement) parallelStepElement.getSections().get(0);
    K8sRollingStepInfo k8sStepInfo = (K8sRollingStepInfo) stepElement.getStepSpecType();
    assertThat(k8sStepInfo.getTimeout()).isInstanceOf(ParameterField.class);
    assertThat(k8sStepInfo.getTimeout().isExpression()).isTrue();
    assertThat(k8sStepInfo.getTimeout().getExpressionValue()).isEqualTo("${input}");
    assertThat(k8sStepInfo.getTimeout().getInputSetValidator().getParameters()).isEqualTo("100, 1000, 100");
    assertThat(k8sStepInfo.getTimeout().getInputSetValidator().getValidatorType())
        .isEqualTo(InputSetValidatorType.ALLOWED_VALUES);
    assertThat(k8sStepInfo.getSkipDryRun()).isInstanceOf(ParameterField.class);
    assertThat(k8sStepInfo.getSkipDryRun().isExpression()).isTrue();
    assertThat(k8sStepInfo.getSkipDryRun().getExpressionValue()).isEqualTo("${input}");
    assertThat(k8sStepInfo.getSkipDryRun().getInputSetValidator().getParameters()).isEqualTo("true, false");
    assertThat(k8sStepInfo.getSkipDryRun().getInputSetValidator().getValidatorType())
        .isEqualTo(InputSetValidatorType.ALLOWED_VALUES);

    stepElement = (StepElement) deploymentStage.getExecution().getRollbackSteps().get(0);
    K8sRollingRollbackStepInfo rollbackStepInfo = (K8sRollingRollbackStepInfo) stepElement.getStepSpecType();
    assertThat(rollbackStepInfo.getTimeout()).isInstanceOf(ParameterField.class);
    assertThat(rollbackStepInfo.getTimeout().isExpression()).isTrue();
    assertThat(rollbackStepInfo.getTimeout().getExpressionValue()).isEqualTo("${input}");
    assertThat(rollbackStepInfo.getTimeout().getInputSetValidator().getParameters()).isEqualTo("100, 1000, 100");
    assertThat(rollbackStepInfo.getTimeout().getInputSetValidator().getValidatorType())
        .isEqualTo(InputSetValidatorType.ALLOWED_VALUES);

    // Second stage
    stageWrapper = ngPipeline.getStages().get(1);
    deploymentStage = (DeploymentStage) ((StageElement) stageWrapper).getStageType();

    // Service
    service = deploymentStage.getService();
    assertThat(service.getUseFromStage()).isNotNull();
    assertThat(service.getUseFromStage().getStage()).isInstanceOf(ParameterField.class);
    assertThat(service.getUseFromStage().getStage().isExpression()).isTrue();
    assertThat(service.getUseFromStage().getStage().getExpressionValue()).isEqualTo("${input}");
    assertThat(service.getUseFromStage().getStage().getInputSetValidator().getParameters()).isEqualTo("^prod*");
    assertThat(service.getUseFromStage().getStage().getInputSetValidator().getValidatorType())
        .isEqualTo(InputSetValidatorType.REGEX);
    StageOverridesConfig stageOverrides = service.getStageOverrides();
    storeConfig = (GitStore) stageOverrides.getManifests().get(0).getManifestAttributes().getStoreConfig();
    assertThat(storeConfig.getConnectorIdentifier()).isInstanceOf(ParameterField.class);
    assertThat(storeConfig.getConnectorIdentifier().isExpression()).isTrue();
    assertThat(storeConfig.getConnectorIdentifier().getExpressionValue()).isEqualTo("${input}");
    assertThat(storeConfig.getConnectorIdentifier().getInputSetValidator()).isNull();
  }
}
