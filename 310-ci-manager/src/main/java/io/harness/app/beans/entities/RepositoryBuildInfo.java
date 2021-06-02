package io.harness.app.beans.entities;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RepositoryBuildInfo {
  private long time;
  private BuildRepositoryCount builds;
}
