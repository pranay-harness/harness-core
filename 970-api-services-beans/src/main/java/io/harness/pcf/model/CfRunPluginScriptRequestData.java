package io.harness.pcf.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class CfRunPluginScriptRequestData {
  private CfRequestConfig cfRequestConfig;
  private String workingDirectory;
  private String finalScriptString;
}
