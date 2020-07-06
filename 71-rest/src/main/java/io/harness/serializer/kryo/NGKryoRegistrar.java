package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.cdng.artifact.bean.DockerArtifactAttributes;
import io.harness.cdng.artifact.bean.DockerArtifactOutcome;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSource;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.connector.DockerhubConnectorConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.GcrArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifact;
import io.harness.cdng.artifact.delegate.task.ArtifactTaskParameters;
import io.harness.cdng.artifact.delegate.task.ArtifactTaskResponse;
import io.harness.cdng.artifact.steps.ArtifactStepParameters;
import io.harness.cdng.common.beans.Tag;
import io.harness.cdng.connectornextgen.tasks.KubernetesConnectionResponse;
import io.harness.cdng.environment.beans.EnvironmentType;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.cdng.k8s.K8sRollingOutcome;
import io.harness.cdng.manifest.state.ManifestListConfig;
import io.harness.cdng.manifest.yaml.FetchType;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.cdng.service.OverrideConfig;
import io.harness.cdng.service.ServiceConfig;
import io.harness.cdng.service.ServiceSpec;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.service.beans.ServiceOutcome.Artifacts;
import io.harness.cdng.service.steps.ServiceStepParameters;
import io.harness.cdng.tasks.manifestFetch.beans.GitFetchFilesConfig;
import io.harness.cdng.tasks.manifestFetch.beans.GitFetchRequest;
import io.harness.cdng.tasks.manifestFetch.step.ManifestFetchOutcome;
import io.harness.serializer.KryoRegistrar;

public class NGKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(ArtifactStepParameters.class, 8001);
    kryo.register(ArtifactTaskParameters.class, 8002);
    kryo.register(ArtifactTaskResponse.class, 8003);
    kryo.register(DockerArtifactSourceAttributes.class, 8004);
    kryo.register(DockerArtifactAttributes.class, 8005);
    kryo.register(DockerhubConnectorConfig.class, 8006);
    kryo.register(DockerArtifactOutcome.class, 8007);
    kryo.register(ServiceStepParameters.class, 8008);
    kryo.register(ArtifactListConfig.class, 8009);
    kryo.register(ServiceConfig.class, 8010);
    kryo.register(DockerHubArtifactConfig.class, 8011);
    kryo.register(GcrArtifactConfig.class, 8012);
    kryo.register(ServiceSpec.class, 8015);
    kryo.register(SidecarArtifact.class, 8016);
    kryo.register(DockerArtifactSource.class, 8017);
    kryo.register(ServiceOutcome.class, 8018);
    kryo.register(Artifacts.class, 8019);
    kryo.register(ManifestListConfig.class, 8020);
    kryo.register(K8sManifest.class, 8021);
    kryo.register(StoreConfig.class, 8022);
    kryo.register(GitStore.class, 8023);
    kryo.register(OverrideConfig.class, 8024);
    kryo.register(GitFetchRequest.class, 8025);
    kryo.register(GitFetchFilesConfig.class, 8026);
    kryo.register(ManifestFetchOutcome.class, 8027);
    kryo.register(K8SDirectInfrastructure.class, 8028);
    kryo.register(EnvironmentYaml.class, 8029);
    kryo.register(FetchType.class, 8030);
    kryo.register(ManifestOutcome.class, 8031);
    kryo.register(Tag.class, 8032);
    kryo.register(EnvironmentType.class, 8033);
    kryo.register(K8sRollingOutcome.class, 8034);
    kryo.register(KubernetesConnectionResponse.class, 8035);
  }
}
