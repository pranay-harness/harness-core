package io.harness.ngpipeline.pipeline.executions.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.artifacts.ArtifactSourceConstants;
import io.harness.pms.contracts.cd.ArtifactSummaryProto;
import io.harness.pms.contracts.cd.GcrArtifactSummaryProto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.CDC)
@Data
@Builder
@JsonTypeName(ArtifactSourceConstants.GCR_NAME)
public class GcrArtifactSummary implements ArtifactSummary {
  String imagePath;
  String tag;

  @Override
  public String getType() {
    return ArtifactSourceConstants.GCR_NAME;
  }

  @Override
  public ArtifactSummaryProto toProto() {
    GcrArtifactSummaryProto.Builder builder = GcrArtifactSummaryProto.newBuilder();
    if (imagePath != null) {
      builder.setImagePath(imagePath);
    }
    if (tag != null) {
      builder.setTag(tag);
    }
    return ArtifactSummaryProto.newBuilder().setGcrArtifactSummary(builder.build()).build();
  }
}
