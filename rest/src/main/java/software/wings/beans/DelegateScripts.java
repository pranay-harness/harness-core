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
  @Transient private String upgradeScript;
  @Transient private String runScript;
  @Transient private String stopScript;
  @Transient private String watchScript;
  @Transient private String stopWatchScript;
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

  public String getUpgradeScript() {
    return upgradeScript;
  }

  public void setUpgradeScript(String upgradeScript) {
    this.upgradeScript = upgradeScript;
  }

  public String getRunScript() {
    return runScript;
  }

  public void setRunScript(String runScript) {
    this.runScript = runScript;
  }

  public String getStopScript() {
    return stopScript;
  }

  public void setStopScript(String stopScript) {
    this.stopScript = stopScript;
  }

  public String getWatchScript() {
    return watchScript;
  }

  public void setWatchScript(String watchScript) {
    this.watchScript = watchScript;
  }

  public String getStopWatchScript() {
    return stopWatchScript;
  }

  public void setStopWatchScript(String stopWatchScript) {
    this.stopWatchScript = stopWatchScript;
  }

  public String getDelegateScript() {
    return delegateScript;
  }

  public void setDelegateScript(String delegateScript) {
    this.delegateScript = delegateScript;
  }

  public String getScriptByName(String fileName) {
    switch (fileName) {
      case "upgrade.sh":
        return getUpgradeScript();
      case "run.sh":
        return getRunScript();
      case "stop.sh":
        return getStopScript();
      case "watch.sh":
        return getWatchScript();
      case "stopwatch.sh":
        return getStopWatchScript();
      case "delegate.sh":
        return getDelegateScript();
      default:
        return null;
    }
  }
}
