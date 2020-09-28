package io.harness.cdng.pipeline.beans.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.yaml.core.Tag;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CDPipelineSummaryResponseDTO {
  String name;
  String identifier;
  String description;
  int numOfStages;
  int numOfErrors; // total number of errors in the last ten days
  List<Integer> deployments; // no of deployments for each of the last 10 days, most recent first
  List<Tag> tags;
}
