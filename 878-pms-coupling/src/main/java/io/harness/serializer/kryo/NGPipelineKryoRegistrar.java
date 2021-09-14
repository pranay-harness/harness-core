/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngpipeline.artifact.bean.DockerArtifactOutcome;
import io.harness.ngpipeline.artifact.bean.EcrArtifactOutcome;
import io.harness.ngpipeline.artifact.bean.GcrArtifactOutcome;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.ngpipeline.status.BuildUpdateType;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(PIPELINE)
public class NGPipelineKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(DockerArtifactOutcome.class, 8007);
    kryo.register(BuildUpdateType.class, 390003);
    kryo.register(BuildStatusUpdateParameter.class, 390004);
    kryo.register(GcrArtifactOutcome.class, 390006);
    kryo.register(EcrArtifactOutcome.class, 390007);
  }
}
