package io.harness.pms.pipeline.resource;

import io.harness.pms.execution.Status;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

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
  Status status;
  EdgeLayoutListDTO edgeLayoutList;
}
