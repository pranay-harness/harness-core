package io.harness.pms.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.gitsync.sdk.EntityGitDetails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.Version;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("PMSPipelineSummaryResponse")
@TargetModule(HarnessModule._890_PMS_CONTRACTS)
@OwnedBy(PIPELINE)
public class PMSPipelineSummaryResponseDTO {
  String name;
  String identifier;
  String description;
  Map<String, String> tags;
  @Version Long version;
  int numOfStages;
  long createdAt;
  long lastUpdatedAt;
  Set<String> modules;
  ExecutionSummaryInfoDTO executionSummaryInfo;
  Map<String, org.bson.Document> filters;
  List<String> stageNames;
  EntityGitDetails gitDetails;
}
