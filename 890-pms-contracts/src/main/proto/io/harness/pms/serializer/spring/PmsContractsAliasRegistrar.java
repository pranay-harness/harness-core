package io.harness.pms.serializer.spring;

import io.harness.pms.advisers.AdviserObtainment;
import io.harness.pms.advisers.AdviserType;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.ambiance.Level;
import io.harness.pms.data.StepOutcomeRef;
import io.harness.pms.execution.ExecutionMode;
import io.harness.pms.execution.failure.FailureInfo;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.refobjects.RefType;
import io.harness.pms.steps.StepType;
import io.harness.spring.AliasRegistrar;

import java.util.Map;

public class PmsContractsAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("ambiance", Ambiance.class);
    orchestrationElements.put("level", Level.class);
    orchestrationElements.put("executionMode", ExecutionMode.class);
    orchestrationElements.put("adviserType", AdviserType.class);
    orchestrationElements.put("adviserObtainment", AdviserObtainment.class);
    orchestrationElements.put("facilitatorType", FacilitatorType.class);
    orchestrationElements.put("facilitatorObtainment", FacilitatorObtainment.class);
    orchestrationElements.put("stepType", StepType.class);
    orchestrationElements.put("refType", RefType.class);
    orchestrationElements.put("failureInfo", FailureInfo.class);
    orchestrationElements.put("stepOutcomeRef", StepOutcomeRef.class);
  }
}
