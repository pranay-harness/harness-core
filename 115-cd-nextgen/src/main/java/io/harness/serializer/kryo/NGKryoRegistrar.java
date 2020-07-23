package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.cdng.artifact.bean.DockerArtifactOutcome;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSource;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSets;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.GcrArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifact;
import io.harness.cdng.artifact.steps.ArtifactStepParameters;
import io.harness.cdng.environment.steps.EnvironmentStepParameters;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.infra.beans.InfraUseFromStage;
import io.harness.cdng.infra.steps.InfraStepParameters;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.cdng.k8s.K8sRollingOutcome;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.ManifestOverrideSets;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.cdng.manifest.yaml.kinds.ValuesManifest;
import io.harness.cdng.service.beans.KubernetesServiceSpec;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.service.beans.ServiceOutcome.ArtifactsOutcome;
import io.harness.cdng.service.beans.ServiceUseFromStage;
import io.harness.cdng.service.beans.ServiceUseFromStage.Overrides;
import io.harness.cdng.service.beans.StageOverridesConfig;
import io.harness.cdng.service.steps.ServiceStepParameters;
import io.harness.cdng.tasks.manifestFetch.step.ManifestFetchOutcome;
import io.harness.serializer.KryoRegistrar;

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
    kryo.register(StageOverridesConfig.class, 8024);
    kryo.register(ManifestFetchOutcome.class, 8027);
    kryo.register(K8SDirectInfrastructure.class, 8028);
    kryo.register(EnvironmentYaml.class, 8029);
    kryo.register(ManifestOutcome.class, 8031);
    kryo.register(K8sRollingOutcome.class, 8034);
    kryo.register(ServiceUseFromStage.class, 8036);
    kryo.register(ValuesManifest.class, 8037);
    kryo.register(Overrides.class, 8038);
    kryo.register(InfraUseFromStage.class, 8039);
    kryo.register(InfraUseFromStage.Overrides.class, 8040);
    kryo.register(EnvironmentStepParameters.class, 8041);
    kryo.register(InfraStepParameters.class, 8042);
    kryo.register(ManifestOverrideSets.class, 8043);
    kryo.register(ArtifactOverrideSets.class, 8044);
    kryo.register(StoreConfigWrapper.class, 8045);
  }
}
