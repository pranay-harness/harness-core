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

  /**
   * Is restart boolean.
   *
   * @return the boolean
   */
  public boolean isRestart() {
    return restart;
  }

  /**
   * Sets restart.
   *
   * @param restart the restart
   */
  public void setRestart(boolean restart) {
    this.restart = restart;
  }

  /**
   * Is enable boolean.
   *
   * @return the boolean
   */
  public boolean isEnable() {
    return enable;
  }

  /**
   * Sets enable.
   *
   * @param enable the enable
   */
  public void setEnable(boolean enable) {
    this.enable = enable;
  }

  /**
   * Is config only boolean.
   *
   * @return the boolean
   */
  public boolean isConfigOnly() {
    return configOnly;
  }

  /**
   * Sets config only.
   *
   * @param configOnly the config only
   */
  public void setConfigOnly(boolean configOnly) {
    this.configOnly = configOnly;
  }

  /**
   * Is backup boolean.
   *
   * @return the boolean
   */
  public boolean isBackup() {
    return backup;
  }

  /**
   * Sets backup.
   *
   * @param backup the backup
   */
  public void setBackup(boolean backup) {
    this.backup = backup;
  }

  /**
   * Gets release.
   *
   * @return the release
   */
  public Release getRelease() {
    return release;
  }

  /**
   * Sets release.
   *
   * @param release the release
   */
  public void setRelease(Release release) {
    this.release = release;
  }

  /**
   * Gets artifact.
   *
   * @return the artifact
   */
  public Artifact getArtifact() {
    return artifact;
  }

  /**
   * Sets artifact.
   *
   * @param artifact the artifact
   */
  public void setArtifact(Artifact artifact) {
    this.artifact = artifact;
  }

  @Override
  public List<CommandUnit> getCommandUnits() {
    return null;
  }
}
