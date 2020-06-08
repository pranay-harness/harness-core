package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.cdng.artifact.bean.DockerArtifact;
import io.harness.cdng.artifact.bean.DockerArtifactAttributes;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.connector.DockerhubConnectorConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig.DockerSpec;
import io.harness.cdng.artifact.bean.yaml.GcrArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.GcrArtifactConfig.GCRSpec;
import io.harness.cdng.artifact.delegate.task.ArtifactTaskParameters;
import io.harness.cdng.artifact.delegate.task.ArtifactTaskResponse;
import io.harness.cdng.artifact.steps.ArtifactStepParameters;
import io.harness.cdng.service.Service;
import io.harness.cdng.service.ServiceSpec;
import io.harness.cdng.service.steps.ServiceStepParameters;
import io.harness.serializer.KryoRegistrar;

public class NGKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    kryo.register(ArtifactStepParameters.class, 8001);
    kryo.register(ArtifactTaskParameters.class, 8002);
    kryo.register(ArtifactTaskResponse.class, 8003);
    kryo.register(DockerArtifactSourceAttributes.class, 8004);
    kryo.register(DockerArtifactAttributes.class, 8005);
    kryo.register(DockerhubConnectorConfig.class, 8006);
    kryo.register(DockerArtifact.class, 8007);
    kryo.register(ServiceStepParameters.class, 8008);
    kryo.register(ArtifactListConfig.class, 8009);
    kryo.register(Service.class, 8010);
    kryo.register(DockerHubArtifactConfig.class, 8011);
    kryo.register(GcrArtifactConfig.class, 8012);
    kryo.register(DockerSpec.class, 8013);
    kryo.register(GCRSpec.class, 8014);
    kryo.register(ServiceSpec.class, 8015);
  }
}
