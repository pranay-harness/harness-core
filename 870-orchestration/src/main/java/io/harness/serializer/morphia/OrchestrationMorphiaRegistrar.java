package io.harness.serializer.morphia;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.beans.SweepingOutputInstance;
import io.harness.engine.interrupts.InterruptCallback;
import io.harness.engine.pms.EngineAdviseCallback;
import io.harness.engine.pms.EngineFacilitationCallback;
import io.harness.engine.progress.EngineProgressCallback;
import io.harness.engine.resume.EngineResumeAllCallback;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.engine.resume.EngineWaitResumeCallback;
import io.harness.engine.resume.EngineWaitRetryCallback;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.state.inspection.ExpressionVariableUsage;
import io.harness.state.inspection.StateInspection;

import java.util.Set;

@OwnedBy(CDC)
public class OrchestrationMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(DelegateTask.class);
    set.add(StateInspection.class);
    set.add(SweepingOutputInstance.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("state.inspection.ExpressionVariableUsage", ExpressionVariableUsage.class);

    // Engine Callback
    h.put("engine.resume.EngineResumeAllCallback", EngineResumeAllCallback.class);
    h.put("engine.resume.EngineResumeCallback", EngineResumeCallback.class);
    h.put("engine.resume.EngineWaitResumeCallback", EngineWaitResumeCallback.class);
    h.put("engine.resume.EngineWaitRetryCallback", EngineWaitRetryCallback.class);
    h.put("engine.pms.EngineFacilitationCallback", EngineFacilitationCallback.class);
    h.put("engine.pms.EngineAdviseCallback", EngineAdviseCallback.class);
    h.put("engine.progress.EngineProgressCallback", EngineProgressCallback.class);
    h.put("engine.interrupts.InterruptCallback", InterruptCallback.class);
  }
}
