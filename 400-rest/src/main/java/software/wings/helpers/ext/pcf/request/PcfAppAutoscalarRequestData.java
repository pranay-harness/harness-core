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
public class PcfAppAutoscalarRequestData {
  private PcfRequestConfig pcfRequestConfig;
  private String autoscalarYml;
  private String autoscalarFilePath;
  private String configPathVar;
  private String applicationName;
  private String applicationGuid;
  private boolean expectedEnabled;
  private int timeoutInMins;
}
