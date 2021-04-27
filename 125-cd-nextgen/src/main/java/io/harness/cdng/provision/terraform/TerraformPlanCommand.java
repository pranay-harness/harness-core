package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(CDP)
public enum TerraformPlanCommand {
  @JsonProperty("Apply") APPLY("Apply"),
  @JsonProperty("Destroy") DESTROY("Destroy");

  private final String displayName;
  TerraformPlanCommand(String displayName) {
    this.displayName = displayName;
  }

  @JsonCreator
  public static TerraformPlanCommand getPlanCommandType(String displayName) {
    for (TerraformPlanCommand value : TerraformPlanCommand.values()) {
      if (value.displayName.equalsIgnoreCase(displayName)) {
        return value;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + displayName);
  }
}
