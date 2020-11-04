package io.harness.beans.build;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@AllArgsConstructor
public class CIPipelineDetails {
  String pipelineName;
  String pipelineIdentifier;
  List<String> pipelineTags;
}
