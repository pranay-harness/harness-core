package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

/**
 *  Application bean class.
 *
 *
 * @author Rishi
 *
 */

@Entity(value = "platforms", noClassnameStored = true)
public class PlatformSoftware extends Base {
  @Reference(idOnly = true) private Application application;

  private boolean standard;
  private String name;
  private String version;
  private String description;

  private String md5;
  private ArtifactSource source;
  private String binaryDocumentId;

  public boolean isStandard() {
    return standard;
  }

  public void setStandard(boolean standard) {
    this.standard = standard;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getBinaryDocumentId() {
    return binaryDocumentId;
  }

  public void setBinaryDocumentId(String binaryDocumentId) {
    this.binaryDocumentId = binaryDocumentId;
  }

  public Application getApplication() {
    return application;
  }

  public void setApplication(Application application) {
    this.application = application;
  }

  public String getMd5() {
    return md5;
  }

  public void setMd5(String md5) {
    this.md5 = md5;
  }

  public ArtifactSource getSource() {
    return source;
  }

  public void setSource(ArtifactSource source) {
    this.source = source;
  }
}
