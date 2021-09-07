package io.harness.app.beans.entities;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BuildInfo {
  private BuildHealth total;
  private BuildHealth success;
  private BuildHealth failed;
}
