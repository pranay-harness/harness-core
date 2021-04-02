package io.harness.terraform.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDP)
public class TerraformPlanCommandRequest {
  List<String> targets;
  List<String> varFilePaths;
  String varParams; // Needed to send inline variable values in CG
  boolean destroySet;
}
