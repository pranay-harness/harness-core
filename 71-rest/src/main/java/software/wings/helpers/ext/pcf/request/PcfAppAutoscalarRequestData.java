package software.wings.helpers.ext.pcf.request;

import lombok.Builder;
import lombok.Data;
import software.wings.helpers.ext.pcf.PcfRequestConfig;

@Data
@Builder
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
