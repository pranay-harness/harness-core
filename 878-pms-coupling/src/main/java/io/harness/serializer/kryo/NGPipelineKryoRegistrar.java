/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
