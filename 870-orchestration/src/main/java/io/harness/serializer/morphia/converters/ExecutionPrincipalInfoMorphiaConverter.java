package io.harness.serializer.morphia.converters;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.contracts.plan.ExecutionPrincipalInfo;

@OwnedBy(HarnessTeam.PIPELINE)
public class ExecutionPrincipalInfoMorphiaConverter extends ProtoMessageConverter<ExecutionPrincipalInfo> {
  public ExecutionPrincipalInfoMorphiaConverter() {
    super(ExecutionPrincipalInfo.class);
  }
}
