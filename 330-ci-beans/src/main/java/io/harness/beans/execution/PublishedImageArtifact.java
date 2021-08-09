package io.harness.beans.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.CI)
public class PublishedImageArtifact {
  String imageName;
  String tag;
  String url;
}
