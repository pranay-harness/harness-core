package io.harness.terraform.request;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TerraformPlanCommandRequest {
  List<String> targets;
  List<String> varFilePaths;
  boolean destroySet;
}
