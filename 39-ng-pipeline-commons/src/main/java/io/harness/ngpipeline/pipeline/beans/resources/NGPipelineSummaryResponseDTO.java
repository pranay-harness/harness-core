package io.harness.ngpipeline.pipeline.beans.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("NGPipelineSummaryResponse")
public class NGPipelineSummaryResponseDTO {
  String name;
  String identifier;
  String description;
  int numOfStages;
  int numOfErrors; // total number of errors in the last ten days
  List<Integer> deployments; // no of deployments for each of the last 10 days, most recent first
  Map<String, String> tags;
}
