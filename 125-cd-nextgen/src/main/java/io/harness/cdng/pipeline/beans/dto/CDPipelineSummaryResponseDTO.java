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
  List<Tag> tags;
}
