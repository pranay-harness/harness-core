package io.harness.terragrunt;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.EqualsAndHashCode;
import org.zeroturnaround.exec.stream.LogOutputStream;

@EqualsAndHashCode(callSuper = false)

@OwnedBy(CDP)
public class PlanJsonLogOutputStream extends LogOutputStream {
  private String planJson;

  @Override
  public void processLine(String line) {
    planJson = line;
  }

  public String getPlanJson() {
    return planJson;
  }
}
