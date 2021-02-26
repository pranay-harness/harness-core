package software.wings.helpers.ext.pcf.request;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.helpers.ext.pcf.PcfRequestConfig;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
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
