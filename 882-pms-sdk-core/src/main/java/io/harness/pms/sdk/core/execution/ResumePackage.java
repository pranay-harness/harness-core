package io.harness.pms.sdk.core.execution;

import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.tasks.ResponseData;

import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class ResumePackage {
  @NonNull NodeExecutionProto nodeExecution;
  Map<String, ResponseData> responseDataMap;
}
