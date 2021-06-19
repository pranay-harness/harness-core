package software.wings.helpers.ext.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pcf.model.CfCliVersion;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
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
  private boolean useCFCLI;
  private String cfCliPath;
  private CfCliVersion cfCliVersion;
  private String cfHomeDirPath;
  private boolean loggedin;
  private boolean limitPcfThreads;
  private boolean ignorePcfConnectionContextCache;
}
