package software.wings.api;

import io.harness.data.SweepingOutput;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TerraformPlanParam implements SweepingOutput {
  private String terraformPlanSecretManagerId;
  private String tfplan;
}
