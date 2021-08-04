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
