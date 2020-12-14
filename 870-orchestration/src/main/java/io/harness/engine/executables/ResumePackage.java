package io.harness.engine.executables;

import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.tasks.ResponseData;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class ResumePackage {
  @NonNull NodeExecutionProto nodeExecution;
  @Singular List<PlanNodeProto> nodes;
  Map<String, ResponseData> responseDataMap;
}
