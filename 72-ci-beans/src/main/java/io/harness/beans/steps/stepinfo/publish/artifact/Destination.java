package io.harness.beans.steps.stepinfo.publish.artifact;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Destination {
  private String location;
  private String connector;
}
