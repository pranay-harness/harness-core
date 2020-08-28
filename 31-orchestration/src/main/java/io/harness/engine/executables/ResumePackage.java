package io.harness.engine.executables;

import io.harness.delegate.beans.ResponseData;
import io.harness.execution.NodeExecution;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class ResumePackage {
  @NonNull NodeExecution nodeExecution;
  Map<String, ResponseData> responseDataMap;
}
