package software.wings.helpers.ext.pcf.request;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.pcf.model.CfRequestConfig;
import lombok.Builder;
import lombok.Data;

import static io.harness.annotations.dev.HarnessTeam.CDP;

@Data
@Builder
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class PcfRunPluginScriptRequestData {
  private CfRequestConfig cfRequestConfig;
  private PcfRunPluginCommandRequest pluginCommandRequest;
  private String workingDirectory;
  private String finalScriptString;
}
