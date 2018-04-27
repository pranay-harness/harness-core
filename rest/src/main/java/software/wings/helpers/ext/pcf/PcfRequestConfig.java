package software.wings.helpers.ext.pcf;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class PcfRequestConfig {
  private String orgName;
  private String spaceName;
  private String userName;
  private String password;
  private String endpointUrl;
  private String applicationName;
  private String manifestYaml;
  private int desiredCount;
  private List<String> routeMaps;
  private Map<String, String> serviceVariables;
  Map<String, String> safeDisplayServiceVariables;
  private int timeOutIntervalInMins;
}