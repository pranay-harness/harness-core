package io.harness.engine.executables;

import io.harness.execution.NodeExecution;
import io.harness.tasks.ResponseData;

import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class ResumePackage {
  @NonNull NodeExecution nodeExecution;
  Map<String, ResponseData> responseDataMap;
}
