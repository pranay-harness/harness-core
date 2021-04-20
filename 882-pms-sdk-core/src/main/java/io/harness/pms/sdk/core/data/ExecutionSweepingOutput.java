package io.harness.pms.sdk.core.data;

import io.harness.pms.contracts.refobjects.RefType;
import io.harness.pms.data.OrchestrationRefType;

public interface ExecutionSweepingOutput extends StepTransput {
  RefType REF_TYPE = RefType.newBuilder().setType(OrchestrationRefType.SWEEPING_OUTPUT).build();
}
