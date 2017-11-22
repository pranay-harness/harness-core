package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.mongodb.morphia.annotations.Transient;

/**
 * Created by brett on 7/26/17
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegateScripts {
  private String delegateId;
  private String version;
  @Transient private boolean doUpgrade;
  @Transient private String stopScript;
  @Transient private String startScript;
  @Transient private String delegateScript;

  public String getDelegateId() {
    return delegateId;
  }

  public void setDelegateId(String delegateId) {
    this.delegateId = delegateId;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public boolean isDoUpgrade() {
    return doUpgrade;
  }

  public void setDoUpgrade(boolean doUpgrade) {
    this.doUpgrade = doUpgrade;
  }

  public String getStopScript() {
    return stopScript;
  }

  public void setStopScript(String stopScript) {
    this.stopScript = stopScript;
  }

  public String getStartScript() {
    return startScript;
  }

  public void setStartScript(String startScript) {
    this.startScript = startScript;
  }

  public String getDelegateScript() {
    return delegateScript;
  }

  public void setDelegateScript(String delegateScript) {
    this.delegateScript = delegateScript;
  }

  public String getScriptByName(String fileName) {
    switch (fileName) {
      case "start.sh":
        return getStartScript();
      case "stop.sh":
        return getStopScript();
      case "delegate.sh":
        return getDelegateScript();
      default:
        return null;
    }
  }
}
