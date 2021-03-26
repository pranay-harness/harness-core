package io.harness.ngtriggers.beans.target.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.target.TargetSpec;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName("Pipeline")
@OwnedBy(PIPELINE)
public class PipelineTargetSpec implements TargetSpec {
  String runtimeInputYaml;
}
