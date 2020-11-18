package io.harness.data;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.refobjects.RefType;
import io.harness.references.OrchestrationRefType;
import io.harness.state.io.StepTransput;

@OwnedBy(CDC)
public interface SweepingOutput extends StepTransput {
  RefType REF_TYPE = RefType.newBuilder().setType(OrchestrationRefType.SWEEPING_OUTPUT).build();
}
