package io.harness.cdng.pipeline.executions.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.cd.InfraExecutionSummaryProto;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class InfraExecutionSummary {
  String identifier;
  String name;

  public InfraExecutionSummaryProto toProto() {
    InfraExecutionSummaryProto.Builder builder = InfraExecutionSummaryProto.newBuilder();
    if (identifier != null) {
      builder.setIdentifier(identifier);
    }
    if (name != null) {
      builder.setName(name);
    }
    return builder.build();
  }
}
