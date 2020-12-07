package io.harness.engine.executables;

import io.harness.pms.execution.NodeExecutionProto;
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
