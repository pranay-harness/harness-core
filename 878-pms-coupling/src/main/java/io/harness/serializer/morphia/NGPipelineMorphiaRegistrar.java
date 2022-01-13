/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer.morphia;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.ngpipeline.artifact.bean.DockerArtifactOutcome;
import io.harness.ngpipeline.artifact.bean.GcrArtifactOutcome;

import java.util.Set;

@OwnedBy(PIPELINE)
public class NGPipelineMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {}

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("ngpipeline.artifact.bean.DockerArtifactOutcome", DockerArtifactOutcome.class);
    h.put("ngpipeline.artifact.bean.GcrArtifactOutcome", GcrArtifactOutcome.class);
  }
}
