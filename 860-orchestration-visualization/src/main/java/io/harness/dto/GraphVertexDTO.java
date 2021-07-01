package io.harness.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.interrupts.InterruptEffect;
import io.harness.logging.UnitProgress;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.run.NodeRunInfo;
import io.harness.pms.contracts.execution.skip.SkipInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import org.bson.Document;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GraphVertexDTO {
  String uuid;
  String identifier;
  String name;
  Long startTs;
  Long endTs;
  String stepType;
  Status status;
  FailureInfoDTO failureInfo;
  SkipInfo skipInfo;
  NodeRunInfo nodeRunInfo;
  Document stepParameters;
  List<ExecutableResponse> executableResponses;
  List<InterruptEffect> interruptHistories;
  Map<String, Document> outcomes;
  List<UnitProgress> unitProgresses;
  Document progressData;
}
