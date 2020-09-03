package io.harness.beans.build;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PublishedArtifact {
  private String buildNumber;
  private String buildLink;
}
