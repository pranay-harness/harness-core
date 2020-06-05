package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.cdng.artifact.bean.DockerArtifact;
import io.harness.cdng.artifact.bean.DockerArtifactAttributes;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.connector.DockerhubConnectorConfig;
import io.harness.cdng.artifact.delegate.task.ArtifactTaskParameters;
import io.harness.cdng.artifact.delegate.task.ArtifactTaskResponse;
import io.harness.cdng.artifact.state.ArtifactStepParameters;
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
  }
}
