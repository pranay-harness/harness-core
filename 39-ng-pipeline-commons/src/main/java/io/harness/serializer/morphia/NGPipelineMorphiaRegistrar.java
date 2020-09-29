package io.harness.serializer.morphia;

import io.harness.cdng.pipeline.beans.entities.CDPipelineEntity;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionSummary;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.ngpipeline.BaseInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;

import java.util.Set;

public class NGPipelineMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(OverlayInputSetEntity.class);
    set.add(CDPipelineEntity.class);
    set.add(PipelineExecutionSummary.class);
    set.add(BaseInputSetEntity.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {}
}
