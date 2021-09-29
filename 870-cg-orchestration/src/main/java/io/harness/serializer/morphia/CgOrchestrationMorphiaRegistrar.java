package io.harness.serializer.morphia;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.beans.ResourceConstraint;
import io.harness.beans.ShellScriptProvisionOutputVariables;
import io.harness.beans.SweepingOutputInstance;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.state.inspection.ExpressionVariableUsage;
import io.harness.state.inspection.StateInspection;

import software.wings.sm.BarrierStatusData;

import java.util.Set;

@OwnedBy(CDC)
public class CgOrchestrationMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(DelegateTask.class);
    set.add(StateInspection.class);
    set.add(SweepingOutputInstance.class);
    set.add(ResourceConstraint.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("state.inspection.ExpressionVariableUsage", ExpressionVariableUsage.class);
    w.put("beans.ShellScriptProvisionOutputVariables", ShellScriptProvisionOutputVariables.class);
    w.put("sm.BarrierStatusData", BarrierStatusData.class);
  }
}
