package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

import java.util.List;

/**
 * The Class Deployment.
 */
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
  public List<CommandUnit> getCommandUnits() {
    return null;
  }
}
