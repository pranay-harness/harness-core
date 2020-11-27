package io.harness.ngtriggers.beans.target.pipeline;

import io.harness.ngtriggers.beans.target.TargetSpec;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName("Pipeline")
public class PipelineTargetSpec implements TargetSpec {
  String runtimeInputYaml;
}
