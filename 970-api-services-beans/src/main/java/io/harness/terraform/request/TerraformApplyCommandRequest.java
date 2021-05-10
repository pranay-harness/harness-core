package io.harness.terraform.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDP)
public class TerraformApplyCommandRequest {
  String planName;
}
