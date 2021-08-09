package io.harness.serializer.spring.converters.principal;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.plan.ExecutionPrincipalInfo;
import io.harness.serializer.spring.ProtoWriteConverter;

@OwnedBy(PIPELINE)
public class ExecutionPrincipalInfoWriteConverter extends ProtoWriteConverter<ExecutionPrincipalInfo> {}
