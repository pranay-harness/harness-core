package io.harness.pms.plan.execution.beans.dto;

import io.harness.pms.execution.ExecutionStatus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.bson.Document;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("GraphLayoutNode")
public class GraphLayoutNodeDTO {
  String nodeType;
  String nodeIdentifier;
  String nodeUuid;
  ExecutionStatus status;
  Map<String, Document> moduleInfo;

  EdgeLayoutListDTO edgeLayoutList;
}
