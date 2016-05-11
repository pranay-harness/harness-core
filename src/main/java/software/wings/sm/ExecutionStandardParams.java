/**
 *
 */
package software.wings.sm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Rishi
 *
 */
public class ExecutionStandardParams implements Serializable {
  private static final long serialVersionUID = 247894502473046682L;
  private String appName;
  private String appDescription;
  private String releaseName;
  private String artifactDisplayName;
  private String artifactRevision;

  private String envName;

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public String getAppDescription() {
    return appDescription;
  }

  public void setAppDescription(String appDescription) {
    this.appDescription = appDescription;
  }

  public String getReleaseName() {
    return releaseName;
  }

  public void setReleaseName(String releaseName) {
    this.releaseName = releaseName;
  }

  public String getArtifactDisplayName() {
    return artifactDisplayName;
  }

  public void setArtifactDisplayName(String artifactDisplayName) {
    this.artifactDisplayName = artifactDisplayName;
  }

  public String getArtifactRevision() {
    return artifactRevision;
  }

  public void setArtifactRevision(String artifactRevision) {
    this.artifactRevision = artifactRevision;
  }

  public String getEnvName() {
    return envName;
  }

  public void setEnvName(String envName) {
    this.envName = envName;
  }

  public Map<String, Object> paramMap() {
    return new HashMap<>();
  }
}
