package io.harness.delegate.task.pcf.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.pcf.model.CfRequestConfig;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class CfRunPluginScriptRequestData {
  private CfRequestConfig cfRequestConfig;
  private CfRunPluginCommandRequest pluginCommandRequest;
  private String workingDirectory;
  private String finalScriptString;
}
