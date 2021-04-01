package io.harness.ngpipeline.pipeline.executions.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.cd.ArtifactsSummaryProto;
import io.harness.pms.contracts.cd.ServiceExecutionSummaryProto;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class ServiceExecutionSummary {
  String identifier;
  String displayName;
  String deploymentType;
  ArtifactsSummary artifacts;

  public ServiceExecutionSummaryProto toProto() {
    ServiceExecutionSummaryProto.Builder builder = ServiceExecutionSummaryProto.newBuilder();
    if (identifier != null) {
      builder.setIdentifier(identifier);
    }
    if (displayName != null) {
      builder.setName(displayName);
    }
    if (deploymentType != null) {
      builder.setDeploymentType(deploymentType);
    }
    if (artifacts != null) {
      builder.setArtifacts(artifacts.toProto());
    }
    return builder.build();
  }

  @Data
  @Builder
  public static class ArtifactsSummary {
    private ArtifactSummary primary;
    @Singular private List<ArtifactSummary> sidecars;

    public ArtifactsSummaryProto toProto() {
      ArtifactsSummaryProto.Builder builder = ArtifactsSummaryProto.newBuilder();
      if (primary != null) {
        builder.setPrimary(primary.toProto());
      }
      if (EmptyPredicate.isNotEmpty(sidecars)) {
        builder.addAllSidecars(
            sidecars.stream().filter(Objects::nonNull).map(ArtifactSummary::toProto).collect(Collectors.toList()));
      }
      return builder.build();
    }
  }
}
