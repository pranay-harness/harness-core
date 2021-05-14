package io.harness.cdng.pipeline;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.ng.core.mapper.TagMapper.convertToList;
import static io.harness.rule.OwnerRule.ARCHIT;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifact;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifactWrapper;
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
import io.harness.cdng.variables.beans.NGVariableOverrideSetWrapper;
import io.harness.cdng.variables.beans.NGVariableOverrideSets;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.validation.InputSetValidatorType;
import io.harness.rule.Owner;
import io.harness.steps.common.script.ShellScriptInlineSource;
import io.harness.steps.common.script.ShellScriptStepParameters;
import io.harness.steps.common.script.ShellType;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.core.StepGroupElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;
import io.harness.yaml.core.failurestrategy.NGFailureType;
import io.harness.yaml.core.failurestrategy.OnFailureConfig;
import io.harness.yaml.core.failurestrategy.manualintervention.ManualInterventionFailureActionConfig;
import io.harness.yaml.core.failurestrategy.retry.RetryFailureActionConfig;
import io.harness.yaml.core.timeout.Timeout;
import io.harness.yaml.core.variables.NGVariableType;
import io.harness.yaml.core.variables.NumberNGVariable;
import io.harness.yaml.core.variables.StringNGVariable;
import io.harness.yaml.utils.YamlPipelineUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDC)
public class PipelineYamlTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  @Ignore("New Test in PMS will be written")
  public void testPipelineWithRuntimeInputYaml() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("cdng/pipelineWithRuntimeInput.yml");
    NgPipeline ngPipeline = YamlPipelineUtils.read(testFile, NgPipeline.class);
    assertThat(ngPipeline.getIdentifier()).isEqualTo("myPipeline1");
    assertThat(ngPipeline.getStages().size()).isEqualTo(2);
    assertThat(ngPipeline.getVariables().size()).isEqualTo(2);

    assertThat(ngPipeline.getVariables().get(0)).isInstanceOf(StringNGVariable.class);
    StringNGVariable stringVariable = (StringNGVariable) ngPipeline.getVariables().get(0);
    assertThat(stringVariable.getName()).isEqualTo("pipelineN1");
    assertThat(stringVariable.getType()).isEqualTo(NGVariableType.STRING);
    assertThat(stringVariable.getValue().getValue()).isEqualTo("stringValue1");
    NumberNGVariable numberNGVariable = (NumberNGVariable) ngPipeline.getVariables().get(1);
    assertThat(numberNGVariable.getName()).isEqualTo("pipelineN2");
    assertThat(numberNGVariable.getType()).isEqualTo(NGVariableType.NUMBER);
    assertThat(numberNGVariable.getValue().getValue()).isEqualTo(11);

    // First Stage
    StageElementWrapper stageWrapper = ngPipeline.getStages().get(0);
    StageElement stageElement = (StageElement) stageWrapper;

    // Stage Failure Strategy
    assertThat(stageElement.getFailureStrategies().size()).isEqualTo(3);

    OnFailureConfig onFailure = stageElement.getFailureStrategies().get(0).getOnFailure();
    assertThat(onFailure.getErrors().size()).isEqualTo(1);
    assertThat(onFailure.getErrors().get(0)).isEqualTo(NGFailureType.TIMEOUT_ERROR);
    assertThat(onFailure.getAction().getType()).isEqualTo(NGFailureActionType.ABORT);

    onFailure = stageElement.getFailureStrategies().get(1).getOnFailure();
    assertThat(onFailure.getErrors().size()).isEqualTo(1);
    assertThat(onFailure.getErrors().get(0)).isEqualTo(NGFailureType.CONNECTIVITY_ERROR);
    assertThat(onFailure.getAction().getType()).isEqualTo(NGFailureActionType.MANUAL_INTERVENTION);
    ManualInterventionFailureActionConfig manualAction = (ManualInterventionFailureActionConfig) onFailure.getAction();
    assertThat(manualAction.getSpecConfig().getTimeout().getValue()).isEqualTo(Timeout.fromString("1d"));
    assertThat(manualAction.getSpecConfig().getOnTimeout().getAction().getType()).isEqualTo(NGFailureActionType.IGNORE);

    onFailure = stageElement.getFailureStrategies().get(2).getOnFailure();
    assertThat(onFailure.getErrors().size()).isEqualTo(1);
    assertThat(onFailure.getErrors().get(0)).isEqualTo(NGFailureType.ANY_OTHER_ERRORS);
    assertThat(onFailure.getAction().getType()).isEqualTo(NGFailureActionType.RETRY);
    RetryFailureActionConfig retryAction = (RetryFailureActionConfig) onFailure.getAction();
    assertThat(retryAction.getSpecConfig().getRetryCount().getValue()).isEqualTo(3);
    assertThat(retryAction.getSpecConfig().getRetryIntervals().getValue().size()).isEqualTo(2);
    assertThat(retryAction.getSpecConfig().getOnRetryFailure().getAction().getType())
        .isEqualTo(NGFailureActionType.ABORT);

    DeploymentStage deploymentStage = (DeploymentStage) stageElement.getStageType();

    // StageVariables
    assertThat(deploymentStage.getVariables().size()).isEqualTo(2);
    stringVariable = (StringNGVariable) deploymentStage.getVariables().get(0);
    assertThat(stringVariable.getName()).isEqualTo("stageN1");
    assertThat(stringVariable.getType()).isEqualTo(NGVariableType.STRING);
    assertThat(stringVariable.getValue().getValue()).isEqualTo("stringValue2");
    numberNGVariable = (NumberNGVariable) deploymentStage.getVariables().get(1);
    assertThat(numberNGVariable.getName()).isEqualTo("stageN2");
    assertThat(numberNGVariable.getType()).isEqualTo(NGVariableType.NUMBER);
    assertThat(numberNGVariable.getValue().getValue()).isEqualTo(12);

    // Service
    ServiceConfig service = deploymentStage.getServiceConfig();

    // Test Service Tags
    List<NGTag> tags = convertToList(service.getService().getTags());
    assertThat(tags.size()).isEqualTo(2);
    Set<String> tagStrings = tags.stream().map(tag -> tag.getKey() + ": " + tag.getValue()).collect(toSet());
    assertThat(tagStrings).containsOnly("k1: v1", "k2: v2");

    KubernetesServiceSpec serviceSpec = (KubernetesServiceSpec) service.getServiceDefinition().getServiceSpec();
    assertThat(serviceSpec).isNotNull();
    assertThat(service.getService().getIdentifier()).isEqualTo("service1");

    // Service Variables
    assertThat(serviceSpec.getVariables().size()).isEqualTo(2);
    stringVariable = (StringNGVariable) serviceSpec.getVariables().get(0);
    assertThat(stringVariable.getName()).isEqualTo("serviceN1");
    assertThat(stringVariable.getType()).isEqualTo(NGVariableType.STRING);
    assertThat(stringVariable.getValue().getValue()).isEqualTo("stringValue3");
    numberNGVariable = (NumberNGVariable) serviceSpec.getVariables().get(1);
    assertThat(numberNGVariable.getName()).isEqualTo("serviceN2");
    assertThat(numberNGVariable.getType()).isEqualTo(NGVariableType.NUMBER);
    assertThat(numberNGVariable.getValue().getValue()).isEqualTo(13);

    // Primary Artifacts
    ArtifactConfig primary = serviceSpec.getArtifacts().getPrimary().getSpec();
    assertThat(primary).isInstanceOf(DockerHubArtifactConfig.class);
    DockerHubArtifactConfig dockerArtifact = (DockerHubArtifactConfig) primary;
    assertThat(dockerArtifact.getImagePath()).isInstanceOf(ParameterField.class);
    assertThat(dockerArtifact.getImagePath().isExpression()).isTrue();
    assertThat(dockerArtifact.getImagePath().getExpressionValue()).isEqualTo("<+input>");

    // Sidecar Artifact
    SidecarArtifactWrapper sidecarArtifactWrapper = serviceSpec.getArtifacts().getSidecars().get(0);
    SidecarArtifact sidecarArtifact = sidecarArtifactWrapper.getSidecar();
    assertThat(sidecarArtifact.getSpec()).isInstanceOf(DockerHubArtifactConfig.class);
    dockerArtifact = (DockerHubArtifactConfig) sidecarArtifact.getSpec();
    assertThat(dockerArtifact.getTag()).isInstanceOf(ParameterField.class);
    assertThat(dockerArtifact.getTag().isExpression()).isTrue();
    assertThat(dockerArtifact.getTag().getExpressionValue()).isEqualTo("<+input>");
    assertThat(dockerArtifact.getTag().getInputSetValidator()).isNull();

    // Manifests
    ManifestConfigWrapper manifestConfigWrapper = serviceSpec.getManifests().get(0);
    ManifestConfig manifestConfig = manifestConfigWrapper.getManifest();
    GitStore storeConfig = (GitStore) manifestConfig.getSpec().getStoreConfig();
    assertThat(storeConfig.getPaths()).isInstanceOf(ParameterField.class);
    assertThat(storeConfig.getPaths().isExpression()).isTrue();
    assertThat(storeConfig.getPaths().getExpressionValue()).isEqualTo("<+input>");
    assertThat(storeConfig.getPaths().getInputSetValidator()).isNotNull();
    assertThat(storeConfig.getPaths().getInputSetValidator().getValidatorType())
        .isEqualTo(InputSetValidatorType.ALLOWED_VALUES);
    assertThat(storeConfig.getPaths().getInputSetValidator().getParameters())
        .isEqualTo("['paths1', 'master/paths2', 'paths3']");

    // manifestOverrideSet
    manifestConfigWrapper = serviceSpec.getManifestOverrideSets().get(0).getOverrideSet().getManifests().get(0);
    manifestConfig = manifestConfigWrapper.getManifest();
    storeConfig = (GitStore) manifestConfig.getSpec().getStoreConfig();
    assertThat(storeConfig.getConnectorRef()).isInstanceOf(ParameterField.class);
    assertThat(storeConfig.getConnectorRef().isExpression()).isTrue();
    assertThat(storeConfig.getConnectorRef().getExpressionValue()).isEqualTo("<+input>");
    assertThat(storeConfig.getPaths().getInputSetValidator()).isNull();

    // VariableOverrideSets
    assertThat(serviceSpec.getVariableOverrideSets().size()).isEqualTo(1);
    NGVariableOverrideSets ngVariableOverrideSets = serviceSpec.getVariableOverrideSets()
                                                        .stream()
                                                        .map(NGVariableOverrideSetWrapper::getOverrideSet)
                                                        .collect(toList())
                                                        .get(0);
    assertThat(ngVariableOverrideSets.getIdentifier()).isEqualTo("VariableoverrideSet");
    assertThat(ngVariableOverrideSets.getVariables().size()).isEqualTo(1);
    numberNGVariable = (NumberNGVariable) ngVariableOverrideSets.getVariables().get(0);
    assertThat(numberNGVariable.getName()).isEqualTo("o1");
    assertThat(numberNGVariable.getType()).isEqualTo(NGVariableType.NUMBER);
    assertThat(numberNGVariable.getValue().getValue()).isEqualTo(14);

    // Infrastructure & environment
    PipelineInfrastructure infrastructure = deploymentStage.getInfrastructure();
    EnvironmentYaml environment = infrastructure.getEnvironment();
    assertThat(environment.getIdentifier()).isEqualTo("env1");
    assertThat(environment.getName()).isEqualTo("env1");

    // Assert Env Tags
    assertThat(environment.getTags().size()).isEqualTo(2);
    assertThat(environment.getTags().keySet()).containsOnly("envType", "envRegion");
    assertThat(environment.getTags().get("envType")).isEqualTo("prod");
    assertThat(environment.getTags().get("envRegion")).isEqualTo("us-east1");

    K8SDirectInfrastructure infraDefinition =
        (K8SDirectInfrastructure) infrastructure.getInfrastructureDefinition().getSpec();
    assertThat(infraDefinition.getNamespace()).isInstanceOf(ParameterField.class);
    assertThat(infraDefinition.getNamespace().isExpression()).isTrue();
    assertThat(infraDefinition.getNamespace().getExpressionValue()).isEqualTo("<+input>");
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
    assertThat(k8sStepInfo.getSkipDryRun()).isInstanceOf(ParameterField.class);
    assertThat(k8sStepInfo.getSkipDryRun().isExpression()).isTrue();
    assertThat(k8sStepInfo.getSkipDryRun().getExpressionValue()).isEqualTo("<+input>");
    assertThat(k8sStepInfo.getSkipDryRun().getInputSetValidator().getParameters()).isEqualTo("true, false");
    assertThat(k8sStepInfo.getSkipDryRun().getInputSetValidator().getValidatorType())
        .isEqualTo(InputSetValidatorType.ALLOWED_VALUES);

    StepGroupElement stepGroupElement = (StepGroupElement) steps.get(1);
    assertThat(stepGroupElement.getFailureStrategies().size()).isEqualTo(1);
    onFailure = stepGroupElement.getFailureStrategies().get(0).getOnFailure();
    assertThat(onFailure.getErrors().size()).isEqualTo(4);
    assertThat(onFailure.getErrors().get(0)).isEqualTo(NGFailureType.AUTHENTICATION_ERROR);
    assertThat(onFailure.getErrors().get(1)).isEqualTo(NGFailureType.AUTHORIZATION_ERROR);
    assertThat(onFailure.getErrors().get(2)).isEqualTo(NGFailureType.VERIFICATION_ERROR);
    assertThat(onFailure.getErrors().get(3)).isEqualTo(NGFailureType.DELEGATE_PROVISIONING_ERROR);
    assertThat(onFailure.getAction().getType()).isEqualTo(NGFailureActionType.ABORT);

    stepElement = (StepElement) steps.get(2);
    assertThat(stepElement.getFailureStrategies().size()).isEqualTo(3);

    onFailure = stepElement.getFailureStrategies().get(0).getOnFailure();
    assertThat(onFailure.getErrors().size()).isEqualTo(1);
    assertThat(onFailure.getErrors().get(0)).isEqualTo(NGFailureType.TIMEOUT_ERROR);
    assertThat(onFailure.getAction().getType()).isEqualTo(NGFailureActionType.STEP_GROUP_ROLLBACK);
    onFailure = stepElement.getFailureStrategies().get(1).getOnFailure();
    assertThat(onFailure.getErrors().size()).isEqualTo(1);
    assertThat(onFailure.getErrors().get(0)).isEqualTo(NGFailureType.CONNECTIVITY_ERROR);
    assertThat(onFailure.getAction().getType()).isEqualTo(NGFailureActionType.STAGE_ROLLBACK);
    onFailure = stepElement.getFailureStrategies().get(2).getOnFailure();
    assertThat(onFailure.getErrors().size()).isEqualTo(1);
    assertThat(onFailure.getErrors().get(0)).isEqualTo(NGFailureType.ANY_OTHER_ERRORS);
    assertThat(onFailure.getAction().getType()).isEqualTo(NGFailureActionType.MARK_AS_SUCCESS);

    stepElement = (StepElement) deploymentStage.getExecution().getRollbackSteps().get(0);
    K8sRollingRollbackStepInfo rollbackStepInfo = (K8sRollingRollbackStepInfo) stepElement.getStepSpecType();

    // Second stage
    stageWrapper = ngPipeline.getStages().get(1);
    stageElement = (StageElement) stageWrapper;
    deploymentStage = (DeploymentStage) stageElement.getStageType();

    // Service
    service = deploymentStage.getServiceConfig();
    assertThat(service.getUseFromStage()).isNotNull();
    assertThat(service.getUseFromStage().getStage()).isEqualTo("prod");
    StageOverridesConfig stageOverrides = service.getStageOverrides();
    storeConfig = (GitStore) stageOverrides.getManifests().get(0).getManifest().getSpec().getStoreConfig();
    assertThat(storeConfig.getConnectorRef()).isInstanceOf(ParameterField.class);
    assertThat(storeConfig.getConnectorRef().isExpression()).isTrue();
    assertThat(storeConfig.getConnectorRef().getExpressionValue()).isEqualTo("<+input>");
    assertThat(storeConfig.getConnectorRef().getInputSetValidator()).isNull();

    // useVariableOverrideSets
    List<String> variablesUseList = stageOverrides.getUseVariableOverrideSets().getValue();
    assertThat(variablesUseList.size()).isEqualTo(1);
    assertThat(variablesUseList.get(0)).isEqualTo("VariableoverrideSet");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testShellScriptStepSerialization() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("cdng/shellScriptStep.yml");
    ShellScriptStepParameters shellScriptStepParameters =
        YamlPipelineUtils.read(testFile, ShellScriptStepParameters.class);
    assertThat(shellScriptStepParameters.getOnDelegate().getValue()).isEqualTo(true);
    assertThat(shellScriptStepParameters.getShell()).isEqualTo(ShellType.Bash);
    assertThat(shellScriptStepParameters.getSource().getType()).isEqualTo("Inline");
    assertThat(((ShellScriptInlineSource) shellScriptStepParameters.getSource().getSpec()).getScript().getValue())
        .isEqualTo("echo hi");
  }
}
