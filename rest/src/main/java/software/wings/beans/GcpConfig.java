package software.wings.beans;

import static software.wings.settings.SettingValue.SettingVariableTypes.GCP;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.settings.SettingValue;

/**
 * Created by bzane on 2/28/17
 */
@JsonTypeName("GCP")
public class GcpConfig extends SettingValue {
  @JsonIgnore @NotEmpty private String serviceAccountKeyFileContent;

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

    private GcpConfigBuilder() {}

    public static GcpConfigBuilder aGcpConfig() {
      return new GcpConfigBuilder();
    }

    public GcpConfigBuilder withServiceAccountKeyFileContent(String serviceAccountKeyFileContent) {
      this.serviceAccountKeyFileContent = serviceAccountKeyFileContent;
      return this;
    }

    public GcpConfigBuilder but() {
      return aGcpConfig().withServiceAccountKeyFileContent(serviceAccountKeyFileContent);
    }

    public GcpConfig build() {
      GcpConfig gcpConfig = new GcpConfig();
      gcpConfig.setServiceAccountKeyFileContent(serviceAccountKeyFileContent);
      return gcpConfig;
    }
  }
}
