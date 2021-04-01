package io.harness.beans.build;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ci.PublishedArtifactProto;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CI)
@Value
@Builder
public class PublishedArtifact {
  private String buildNumber;
  private String buildLink;

  public PublishedArtifactProto toProto() {
    PublishedArtifactProto.Builder builder = PublishedArtifactProto.newBuilder();
    if (buildNumber != null) {
      builder.setBuildNumber(buildNumber);
    }
    if (buildLink != null) {
      builder.setBuildLink(buildLink);
    }
    return builder.build();
  }
}
