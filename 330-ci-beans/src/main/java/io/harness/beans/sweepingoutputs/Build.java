package io.harness.beans.sweepingoutputs;

import lombok.Value;

@Value
public class Build {
  String type;
  public Build(String type) {
    this.type = type;
  }
}
