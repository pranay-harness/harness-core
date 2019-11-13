package software.wings.helpers.ext.pcf.request;

import lombok.Builder;
import lombok.Data;
import software.wings.helpers.ext.pcf.PcfRequestConfig;

@Data
@Builder
public class PcfRunPluginScriptRequestData {
  private PcfRequestConfig pcfRequestConfig;
  private PcfRunPluginCommandRequest pluginCommandRequest;
  private String workingDirectory;
  private String finalScriptString;
}
