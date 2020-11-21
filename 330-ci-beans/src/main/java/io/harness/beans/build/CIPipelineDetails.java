package io.harness.beans.build;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class CIPipelineDetails {
  String pipelineName;
  String pipelineIdentifier;
  List<String> pipelineTags;
}
