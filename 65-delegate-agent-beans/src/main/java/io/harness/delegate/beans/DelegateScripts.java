package io.harness.delegate.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

/**
 * Created by brett on 7/26/17
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class DelegateScripts {
  private String version;
  private boolean doUpgrade;
  private String stopScript;
  private String startScript;
  private String delegateScript;
  private String setupProxyScript;

  public String getScriptByName(String fileName) {
    switch (fileName) {
      case "start.sh":
        return getStartScript();
      case "stop.sh":
        return getStopScript();
      case "delegate.sh":
        return getDelegateScript();
      case "setup-proxy.sh":
        return getSetupProxyScript();
      default:
        return null;
    }
  }
}
