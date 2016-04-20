package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;
import software.wings.app.WingsBootstrap;

@Entity(value = "deployments", noClassnameStored = true)
public class Deployment extends Execution {
  @Reference(idOnly = true) private Release release;

  @Reference(idOnly = true) private Artifact artifact;

  private boolean restart = true;
  private boolean enable = true;
  private boolean configOnly;
  private boolean backup = true;

  public boolean isRestart() {
    return restart;
  }

  public void setRestart(boolean restart) {
    this.restart = restart;
  }

  public boolean isEnable() {
    return enable;
  }

  public void setEnable(boolean enable) {
    this.enable = enable;
  }

  public boolean isConfigOnly() {
    return configOnly;
  }

  public void setConfigOnly(boolean configOnly) {
    this.configOnly = configOnly;
  }

  public boolean isBackup() {
    return backup;
  }

  public void setBackup(boolean backup) {
    this.backup = backup;
  }

  @Override
  public String getCommand() {
    // TODO - get from config
    String portalUrl = WingsBootstrap.getConfig().getPortal().getUrl();
    String fwURL = portalUrl + "/bins/framework";
    String params = String.format("%s/configs/download/%s %s %s", portalUrl, getRelease().getApplication().getUuid(),
        getRelease().getUuid(), getArtifact().getUuid());

    return "mkdir -p $HOME/wings_temp && cd $HOME/wings_temp"
        + " && curl -sk -o wings_main.pl " + fwURL + " && chmod a+x wings_main.pl && ./wings_main.pl " + params
        + " && echo \"SUCCESS\"";
  }

  public Release getRelease() {
    return release;
  }

  public void setRelease(Release release) {
    this.release = release;
  }

  public Artifact getArtifact() {
    return artifact;
  }

  public void setArtifact(Artifact artifact) {
    this.artifact = artifact;
  }

  @Override
  public String getSetupCommand() {
    return "rm -rf wings && mkdir -p $HOME/wings && cd $HOME/wings && mkdir -p downloads"; // TODO: Read deployment dir
                                                                                           // location from config
  }

  @Override
  public String getDeployCommand() {
    return String.format("cd wings && mkdir -p runtime && cd runtime && tar -xvzf ../downloads/%s",
        getArtifact().getArtifactFile().getFileName());
  }
}
