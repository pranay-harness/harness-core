package io.harness.app.beans.entities;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RepositoryBuildInfo {
  private String time;
  private BuildRepositoryCount builds;
}
