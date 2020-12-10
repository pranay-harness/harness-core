package io.harness.pms.sdk.core.data;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.data.OrchestrationRefType;
import io.harness.pms.refobjects.RefType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@OwnedBy(CDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface SweepingOutput extends StepTransput {
  RefType REF_TYPE = RefType.newBuilder().setType(OrchestrationRefType.SWEEPING_OUTPUT).build();
}
