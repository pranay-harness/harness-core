package io.harness.ngpipeline.pipeline.executions.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ngpipeline.artifact.bean.ArtifactTypes;
import io.harness.pms.contracts.cd.ArtifactSummaryProto;
import io.harness.pms.contracts.cd.DockerArtifactSummaryProto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.CDC)
@Data
@Builder
@JsonTypeName(ArtifactTypes.DOCKER_ARTIFACT)
public class DockerArtifactSummary implements ArtifactSummary {
  String imagePath;
  String tag;

  @Override
  public String getType() {
    return ArtifactTypes.DOCKER_ARTIFACT;
  }

  @Override
  public ArtifactSummaryProto toProto() {
    DockerArtifactSummaryProto.Builder builder = DockerArtifactSummaryProto.newBuilder();
    if (imagePath != null) {
      builder.setImagePath(imagePath);
    }
    if (tag != null) {
      builder.setTag(tag);
    }
    return ArtifactSummaryProto.newBuilder().setDockerArtifactSummary(builder.build()).build();
  }
}
