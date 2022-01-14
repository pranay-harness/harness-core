package io.harness.serializer.morphia;

import io.harness.beans.OrchestrationEventLog;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;

import java.util.Set;

public class OrchestrationVisualizationMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(OrchestrationEventLog.class);
    set.add(PipelineExecutionSummaryEntity.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {}
}
