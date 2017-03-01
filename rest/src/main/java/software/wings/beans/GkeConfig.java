package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import software.wings.settings.SettingValue;

/**
 * Created by bzane on 2/28/17
 */
@JsonTypeName("GKE")
public class GkeConfig extends SettingValue {
  @Attributes(title = "Project ID") private String projectId;
  @Attributes(title = "Zone") private String zone;
  @Attributes(title = "Cluster name") private String clusterName;
  @Attributes(title = "App name") private String appName;

  /**
   * Instantiates a new setting value.
   */
  public GkeConfig() {
    super(SettingVariableTypes.GKE.name());
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getZone() {
    return zone;
  }

  public void setZone(String zone) {
    this.zone = zone;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String projectId;
    private String zone;
    private String clusterName;
    private String appName;

    private Builder() {}

    /**
     * A kubernetes config builder.
     *
     * @return the builder
     */
    public static Builder aGkeConfig() {
      return new Builder();
    }

    /**
     */
    public Builder withProjectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    /**
     */
    public Builder withZone(String zone) {
      this.zone = zone;
      return this;
    }

    /**
     * @return the builder
     */
    public Builder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    /**
     * @return the builder
     */
    public Builder withAppName(String appName) {
      this.appName = appName;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aGkeConfig().withProjectId(projectId).withZone(zone).withClusterName(clusterName).withAppName(appName);
    }

    /**
     * Build Gke config.
     *
     * @return the Gke config
     */
    public GkeConfig build() {
      GkeConfig gkeConfig = new GkeConfig();
      gkeConfig.setProjectId(projectId);
      gkeConfig.setZone(zone);
      gkeConfig.setClusterName(clusterName);
      gkeConfig.setAppName(appName);
      return gkeConfig;
    }
  }
}
