package software.wings.helpers.ext.pcf.request;

import software.wings.helpers.ext.pcf.PcfRequestConfig;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PcfRunPluginScriptRequestData {
  private PcfRequestConfig pcfRequestConfig;
  private PcfRunPluginCommandRequest pluginCommandRequest;
  private String workingDirectory;
  private String finalScriptString;
}
