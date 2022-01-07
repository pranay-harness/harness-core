/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.advisers.RollbackCustomStepParameters;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSource;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSetWrapper;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSets;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.EcrArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.GcrArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.PrimaryArtifact;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifact;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifactWrapper;
import io.harness.cdng.artifact.steps.ArtifactStepParameters;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.helm.HelmDeployStepInfo;
import io.harness.cdng.helm.HelmDeployStepParams;
import io.harness.cdng.helm.NativeHelmStepPassThroughData;
import io.harness.cdng.helm.rollback.HelmRollbackStepInfo;
import io.harness.cdng.helm.rollback.HelmRollbackStepParams;
import io.harness.cdng.infra.InfrastructureDef;
import io.harness.cdng.infra.beans.InfraUseFromStage;
import io.harness.cdng.infra.steps.InfraSectionStepParameters;
import io.harness.cdng.infra.steps.InfraStepParameters;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.cdng.infra.yaml.K8sGcpInfrastructure;
import io.harness.cdng.k8s.DeleteResourcesWrapper;
import io.harness.cdng.k8s.K8sBlueGreenOutcome;
import io.harness.cdng.k8s.K8sCanaryOutcome;
import io.harness.cdng.k8s.K8sCanaryStepInfo;
import io.harness.cdng.k8s.K8sCanaryStepParameters;
import io.harness.cdng.k8s.K8sDeleteStepInfo;
import io.harness.cdng.k8s.K8sDeleteStepParameters;
import io.harness.cdng.k8s.K8sInstanceUnitType;
import io.harness.cdng.k8s.K8sRollingOutcome;
import io.harness.cdng.k8s.K8sRollingRollbackStepInfo;
import io.harness.cdng.k8s.K8sRollingRollbackStepParameters;
import io.harness.cdng.k8s.K8sRollingStepInfo;
import io.harness.cdng.k8s.K8sRollingStepParameters;
import io.harness.cdng.k8s.K8sScaleStepInfo;
import io.harness.cdng.k8s.K8sScaleStepParameter;
import io.harness.cdng.k8s.K8sStepPassThroughData;
import io.harness.cdng.k8s.beans.GitFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.HelmValuesFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.K8sExecutionPassThroughData;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.manifest.ManifestConfigType;
import io.harness.cdng.manifest.yaml.BitbucketStore;
import io.harness.cdng.manifest.yaml.GcsStoreConfig;
import io.harness.cdng.manifest.yaml.GitLabStore;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.cdng.manifest.yaml.HttpStoreConfig;
import io.harness.cdng.manifest.yaml.ManifestConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.ManifestOverrideSetWrapper;
import io.harness.cdng.manifest.yaml.ManifestOverrideSets;
import io.harness.cdng.manifest.yaml.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.S3StoreConfig;
import io.harness.cdng.manifest.yaml.kinds.HelmChartManifest;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.cdng.manifest.yaml.kinds.KustomizeManifest;
import io.harness.cdng.manifest.yaml.kinds.KustomizePatchesManifest;
import io.harness.cdng.manifest.yaml.kinds.OpenshiftManifest;
import io.harness.cdng.manifest.yaml.kinds.OpenshiftParamManifest;
import io.harness.cdng.manifest.yaml.kinds.ValuesManifest;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.pipeline.beans.DeploymentStageStepParameters;
import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters;
import io.harness.cdng.pipeline.executions.CDAccountExecutionMetadata;
import io.harness.cdng.provision.terraform.TerraformApplyStepInfo;
import io.harness.cdng.provision.terraform.TerraformPlanStepInfo;
import io.harness.cdng.service.beans.KubernetesServiceSpec;
import io.harness.cdng.service.beans.NativeHelmServiceSpec;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceConfigOutcome;
import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.cdng.service.beans.ServiceUseFromStage;
import io.harness.cdng.service.beans.ServiceUseFromStage.Overrides;
import io.harness.cdng.service.beans.ServiceYaml;
import io.harness.cdng.service.beans.StageOverridesConfig;
import io.harness.cdng.service.steps.ServiceStepParameters;
import io.harness.cdng.tasks.manifestFetch.step.ManifestFetchOutcome;
import io.harness.cdng.tasks.manifestFetch.step.ManifestFetchParameters;
import io.harness.cdng.variables.beans.NGVariableOverrideSetWrapper;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(CDC)
public class NGKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(ArtifactStepParameters.class, 8001);
    kryo.register(ServiceStepParameters.class, 8008);
    kryo.register(ArtifactListConfig.class, 8009);
    kryo.register(ServiceConfig.class, 8010);
    kryo.register(DockerHubArtifactConfig.class, 8011);
    kryo.register(GcrArtifactConfig.class, 8012);
    kryo.register(KubernetesServiceSpec.class, 8015);
    kryo.register(SidecarArtifact.class, 8016);
    kryo.register(DockerArtifactSource.class, 8017);
    kryo.register(K8sManifest.class, 8021);
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

    kryo.register(DeploymentStageStepParameters.class, 8047);
    kryo.register(K8sRollingRollbackStepInfo.class, 8049);
    kryo.register(K8sRollingRollbackStepParameters.class, 8050);
    kryo.register(K8sRollingStepInfo.class, 8051);
    kryo.register(K8sRollingStepParameters.class, 8052);
    kryo.register(ManifestFetchParameters.class, 8053);
    kryo.register(K8sStepPassThroughData.class, 8056);

    // Starting using 8100 series
    kryo.register(PipelineInfrastructure.class, 8101);
    kryo.register(InfrastructureDef.class, 8102);
    kryo.register(ServiceDefinition.class, 8103);
    kryo.register(ManifestConfig.class, 8104);
    kryo.register(PrimaryArtifact.class, 8106);
    kryo.register(RollbackOptionalChildChainStepParameters.class, 8108);
    kryo.register(RollbackNode.class, 8109);

    kryo.register(K8sGcpInfrastructure.class, 8301);

    // Starting using 12500 series as 8100 series is also used in 400-rest
    kryo.register(K8sBlueGreenOutcome.class, 12500);
    kryo.register(ServiceConfigOutcome.class, 12508);
    kryo.register(ArtifactOverrideSetWrapper.class, 12509);
    kryo.register(ManifestOverrideSetWrapper.class, 12510);
    kryo.register(NGVariableOverrideSetWrapper.class, 12511);
    kryo.register(K8sInstanceUnitType.class, 12512);
    kryo.register(K8sScaleStepInfo.class, 12513);
    kryo.register(K8sScaleStepParameter.class, 12514);
    kryo.register(K8sCanaryOutcome.class, 12515);
    kryo.register(K8sCanaryStepInfo.class, 12516);
    kryo.register(K8sCanaryStepParameters.class, 12517);
    kryo.register(DeleteResourcesWrapper.class, 12519);
    kryo.register(K8sDeleteStepParameters.class, 12520);
    kryo.register(K8sDeleteStepInfo.class, 12521);
    kryo.register(GitFetchResponsePassThroughData.class, 12522);
    kryo.register(HelmChartManifest.class, 12523);
    kryo.register(GithubStore.class, 12527);
    kryo.register(GitLabStore.class, 12528);
    kryo.register(BitbucketStore.class, 12529);
    kryo.register(HttpStoreConfig.class, 12530);
    kryo.register(KustomizeManifest.class, 12531);
    kryo.register(EcrArtifactConfig.class, 12533);
    kryo.register(OpenshiftManifest.class, 12534);
    kryo.register(OpenshiftParamManifest.class, 12536);
    kryo.register(S3StoreConfig.class, 12538);
    kryo.register(GcsStoreConfig.class, 12539);
    kryo.register(RollbackCustomStepParameters.class, 12540);
    kryo.register(TerraformApplyStepInfo.class, 12541);
    kryo.register(NativeHelmServiceSpec.class, 12542);
    kryo.register(TerraformPlanStepInfo.class, 12543);
    kryo.register(HelmValuesFetchResponsePassThroughData.class, 12544);
    kryo.register(StepExceptionPassThroughData.class, 12545);

    kryo.register(HelmDeployStepInfo.class, 13001);
    kryo.register(HelmDeployStepParams.class, 13002);
    kryo.register(NativeHelmStepPassThroughData.class, 13003);
    kryo.register(HelmRollbackStepInfo.class, 13004);
    kryo.register(HelmRollbackStepParams.class, 13005);

    kryo.register(StoreConfigWrapper.class, 8045);

    kryo.register(K8sExecutionPassThroughData.class, 12546);
    kryo.register(KustomizePatchesManifest.class, 12549);
    kryo.register(CDAccountExecutionMetadata.class, 12550);

    kryo.register(ServiceDefinitionType.class, 12551);
    kryo.register(InfraSectionStepParameters.class, 12552);
    kryo.register(ServiceYaml.class, 12553);
    kryo.register(SidecarArtifactWrapper.class, 12554);
    kryo.register(ManifestConfigWrapper.class, 12555);
    kryo.register(StoreConfigType.class, 12556);
    kryo.register(ManifestConfigType.class, 12557);
  }
}
