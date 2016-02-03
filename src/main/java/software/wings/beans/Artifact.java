package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Reference;

/**
 *  Artifact bean class.
 *
 *
 * @author Rishi
 *
 */
@Entity(value = "artifacts", noClassnameStored = true)
public class Artifact extends Base {
  public enum Status { NEW, RUNNING, QUEUED, WAITING, READY, ABORTED, FAILED, ERROR }

  @Indexed @Reference(idOnly = true) private Application application;

  @Indexed @Reference(idOnly = true) private Release release;

  @Indexed private String compName;

  @Indexed private String artifactSourceName;

  @Indexed private String displayName;

  @Indexed private String revision;

  private ArtifactFile artifactFile;

  @Indexed private Status status;

  public Application getApplication() {
    return application;
  }

  public void setApplication(Application application) {
    this.application = application;
  }

  public Release getRelease() {
    return release;
  }

  public void setRelease(Release release) {
    this.release = release;
  }

  public String getCompName() {
    return compName;
  }

  public void setCompName(String compName) {
    this.compName = compName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getRevision() {
    return revision;
  }

  public void setRevision(String revision) {
    this.revision = revision;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public String getArtifactSourceName() {
    return artifactSourceName;
  }

  public void setArtifactSourceName(String artifactSourceName) {
    this.artifactSourceName = artifactSourceName;
  }

  public ArtifactFile getArtifactFile() {
    return artifactFile;
  }

  public void setArtifactFile(ArtifactFile artifactFile) {
    this.artifactFile = artifactFile;
  }
}
