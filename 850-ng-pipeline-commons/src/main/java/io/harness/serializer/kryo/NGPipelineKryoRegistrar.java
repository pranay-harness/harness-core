package io.harness.serializer.kryo;

import io.harness.ngpipeline.artifact.bean.DockerArtifactOutcome;
import io.harness.ngpipeline.artifact.bean.EcrArtifactOutcome;
import io.harness.ngpipeline.artifact.bean.GcrArtifactOutcome;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.ngpipeline.status.BuildChecksUpdateParameter;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.ngpipeline.status.BuildUpdateType;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class NGPipelineKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(DockerArtifactOutcome.class, 8007);
    kryo.register(NgPipeline.class, 390001);
    kryo.register(BuildUpdateType.class, 390003);
    kryo.register(BuildStatusUpdateParameter.class, 390004);
    kryo.register(BuildChecksUpdateParameter.class, 390005);
    kryo.register(GcrArtifactOutcome.class, 390006);
    kryo.register(EcrArtifactOutcome.class, 390007);
  }
}
