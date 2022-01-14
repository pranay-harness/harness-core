package io.harness.delegate.task.helm;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class HelmChartInfo {
  private String name;
  private String version;
  private String repoUrl;
}
