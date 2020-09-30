package io.harness.cdng.pipeline.beans.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.yaml.core.Tag;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("NGPipelineSummaryResponse")
public class CDPipelineSummaryResponseDTO {
  String name;
  String identifier;
  String description;
  int numOfStages;
  int numOfErrors; // total number of errors in the last ten days
  List<Integer> deployments; // no of deployments for each of the last 10 days, most recent first
  List<Tag> tags;
}
