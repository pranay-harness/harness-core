package io.harness.serializer.kryo;

import io.harness.cdng.artifact.bean.ArtifactSpecWrapper;
import io.harness.cdng.artifact.bean.DockerArtifactOutcome;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSource;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSets;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.GcrArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifact;
import io.harness.cdng.artifact.steps.ArtifactStepParameters;
import io.harness.cdng.environment.EnvironmentOutcome;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.infra.InfrastructureDef;
import io.harness.cdng.infra.beans.InfraUseFromStage;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.infra.steps.InfraStepParameters;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.cdng.k8s.K8sBlueGreenOutcome;
import io.harness.cdng.k8s.K8sRollingOutcome;
import io.harness.cdng.k8s.K8sRollingRollbackStepInfo;
import io.harness.cdng.k8s.K8sRollingRollbackStepParameters;
import io.harness.cdng.k8s.K8sRollingStepInfo;
import io.harness.cdng.k8s.K8sRollingStepParameters;
import io.harness.cdng.k8s.K8sStepPassThroughData;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.K8sManifestOutcome;
import io.harness.cdng.manifest.yaml.ManifestConfig;
import io.harness.cdng.manifest.yaml.ManifestOverrideSets;
import io.harness.cdng.manifest.yaml.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.cdng.manifest.yaml.ValuesManifestOutcome;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.cdng.manifest.yaml.kinds.ValuesManifest;
import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.pipeline.beans.CDPipelineSetupParameters;
import io.harness.cdng.pipeline.beans.DeploymentStageStepParameters;
import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters;
import io.harness.cdng.pipeline.stepinfo.HttpStepInfo;
import io.harness.cdng.pipeline.stepinfo.ShellScriptStepInfo;
import io.harness.cdng.service.beans.KubernetesServiceSpec;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.service.beans.ServiceOutcome.ArtifactsOutcome;
import io.harness.cdng.service.beans.ServiceUseFromStage;
import io.harness.cdng.service.beans.ServiceUseFromStage.Overrides;
import io.harness.cdng.service.beans.StageOverridesConfig;
import io.harness.cdng.service.steps.ServiceStep.ServiceStepPassThroughData;
import io.harness.cdng.service.steps.ServiceStepParameters;
import io.harness.cdng.tasks.manifestFetch.step.ManifestFetchOutcome;
import io.harness.cdng.tasks.manifestFetch.step.ManifestFetchParameters;
import io.harness.http.HttpOutcome;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class NGKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(ArtifactStepParameters.class, 8001);
    kryo.register(DockerArtifactOutcome.class, 8007);
    kryo.register(ServiceStepParameters.class, 8008);
    kryo.register(ArtifactListConfig.class, 8009);
    kryo.register(ServiceConfig.class, 8010);
    kryo.register(DockerHubArtifactConfig.class, 8011);
    kryo.register(GcrArtifactConfig.class, 8012);
    kryo.register(KubernetesServiceSpec.class, 8015);
    kryo.register(SidecarArtifact.class, 8016);
    kryo.register(DockerArtifactSource.class, 8017);
    kryo.register(ServiceOutcome.class, 8018);
    kryo.register(ArtifactsOutcome.class, 8019);
    kryo.register(K8sManifest.class, 8021);
    kryo.register(StoreConfig.class, 8022);
    kryo.register(GitStore.class, 8023);
    kryo.register(StageOverridesConfig.class, 8024);
    kryo.register(ManifestFetchOutcome.class, 8027);
    kryo.register(K8SDirectInfrastructure.class, 8028);
    kryo.register(EnvironmentYaml.class, 8029);
    kryo.register(ManifestsOutcome.class, 8031);
    kryo.register(K8sRollingOutcome.class, 8034);
    kryo.register(ServiceUseFromStage.class, 8036);
    kryo.register(ValuesManifest.class, 8037);
    kryo.register(Overrides.class, 8038);
    kryo.register(InfraUseFromStage.class, 8039);
    kryo.register(InfraUseFromStage.Overrides.class, 8040);
    kryo.register(InfraStepParameters.class, 8042);
    kryo.register(ManifestOverrideSets.class, 8043);
    kryo.register(ArtifactOverrideSets.class, 8044);
    kryo.register(StoreConfigWrapper.class, 8045);
    kryo.register(CDPipelineSetupParameters.class, 8046);
    kryo.register(DeploymentStageStepParameters.class, 8047);
    kryo.register(HttpStepInfo.class, 8048);
    kryo.register(K8sRollingRollbackStepInfo.class, 8049);
    kryo.register(K8sRollingRollbackStepParameters.class, 8050);
    kryo.register(K8sRollingStepInfo.class, 8051);
    kryo.register(K8sRollingStepParameters.class, 8052);
    kryo.register(ManifestFetchParameters.class, 8053);
    kryo.register(ShellScriptStepInfo.class, 8055);
    kryo.register(K8sStepPassThroughData.class, 8056);
    kryo.register(ServiceStepPassThroughData.class, 8057);

    // Starting using 8100 series
    kryo.register(DeploymentStage.class, 8100);
    kryo.register(PipelineInfrastructure.class, 8101);
    kryo.register(InfrastructureDef.class, 8102);
    kryo.register(ServiceDefinition.class, 8103);
    kryo.register(ManifestConfig.class, 8104);
    kryo.register(K8sDirectInfrastructureOutcome.class, 8105);
    kryo.register(ArtifactSpecWrapper.class, 8106);
    kryo.register(EnvironmentOutcome.class, 8107);
    kryo.register(RollbackOptionalChildChainStepParameters.class, 8108);
    kryo.register(RollbackNode.class, 8109);

    // Starting using 12500 series as 8100 series is also used in 400-rest
    kryo.register(K8sBlueGreenOutcome.class, 12500);
    kryo.register(HttpOutcome.class, 12501);
    kryo.register(K8sManifestOutcome.class, 12502);
    kryo.register(ValuesManifestOutcome.class, 12503);
  }
}
