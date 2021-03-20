package software.wings.helpers.ext.pcf.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.helpers.ext.pcf.PcfRequestConfig;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class PcfRunPluginScriptRequestData {
  private PcfRequestConfig pcfRequestConfig;
  private PcfRunPluginCommandRequest pluginCommandRequest;
  private String workingDirectory;
  private String finalScriptString;
}
