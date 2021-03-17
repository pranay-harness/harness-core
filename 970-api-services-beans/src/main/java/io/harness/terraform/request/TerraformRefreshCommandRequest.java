package io.harness.terraform.request;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TerraformRefreshCommandRequest {
  List<String> targets;
  List<String> varFilePaths;
}
