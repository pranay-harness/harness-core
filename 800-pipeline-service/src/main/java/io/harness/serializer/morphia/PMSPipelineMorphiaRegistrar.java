package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.preflight.entity.PreFlightEntity;
import io.harness.pms.sdk.PmsSdkInstance;

import java.util.Set;

public class PMSPipelineMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(PipelineEntity.class);
    set.add(PmsSdkInstance.class);
    set.add(InputSetEntity.class);
    set.add(PipelineExecutionSummaryEntity.class);
    set.add(PreFlightEntity.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {}
}
