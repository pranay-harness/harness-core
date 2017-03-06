package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import software.wings.settings.SettingValue;

import static software.wings.settings.SettingValue.SettingVariableTypes.GCP;

/**
 * Created by bzane on 2/28/17
 */
@JsonTypeName("GCP")
public class GcpConfig extends SettingValue {
  @JsonIgnore private String serviceAccountKeyFileContent;

  public GcpConfig() {
    super(GCP.name());
  }

  public String getServiceAccountKeyFileContent() {
    return serviceAccountKeyFileContent;
  }

  public void setServiceAccountKeyFileContent(String serviceAccountKeyFileContent) {
    this.serviceAccountKeyFileContent = serviceAccountKeyFileContent;
  }

  /**
   * Builder for GcpConfig.
   */
  public static final class GcpConfigBuilder {
    private String serviceAccountKeyFileContent;
    private String type;

    private GcpConfigBuilder() {}

    public static GcpConfigBuilder aGcpConfig() {
      return new GcpConfigBuilder();
    }

    public GcpConfigBuilder withServiceAccountKeyFileContent(String serviceAccountKeyFileContent) {
      this.serviceAccountKeyFileContent = serviceAccountKeyFileContent;
      return this;
    }

    public GcpConfigBuilder withType(String type) {
      this.type = type;
      return this;
    }

    public GcpConfigBuilder but() {
      return aGcpConfig().withServiceAccountKeyFileContent(serviceAccountKeyFileContent).withType(type);
    }

    public GcpConfig build() {
      GcpConfig gcpConfig = new GcpConfig();
      gcpConfig.setServiceAccountKeyFileContent(serviceAccountKeyFileContent);
      gcpConfig.setType(type);
      return gcpConfig;
    }
  }
}
