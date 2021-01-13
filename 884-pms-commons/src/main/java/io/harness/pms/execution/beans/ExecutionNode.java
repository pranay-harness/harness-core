package io.harness.pms.execution.beans;

import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.tasks.ProgressData;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import org.bson.Document;

@Value
@Builder
public class ExecutionNode {
  String uuid;
  String name;
  String identifier;
  String baseFqn;
  List<Document> outcomes;
  Map<String, Object> stepParameters;
  Long startTs;
  Long endTs;
  String stepType;
  ExecutionStatus status;
  FailureInfo failureInfo;
  List<ExecutableResponse> executableResponses;
  Map<String, List<ProgressData>> taskIdToProgressDataMap;
}
