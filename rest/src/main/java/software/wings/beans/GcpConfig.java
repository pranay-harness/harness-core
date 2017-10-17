package software.wings.beans;

import static software.wings.settings.SettingValue.SettingVariableTypes.GCP;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.settings.SettingValue;

/**
 * Created by bzane on 2/28/17
 */
@JsonTypeName("GCP")
@Data
@Builder
public class GcpConfig extends SettingValue {
  @JsonIgnore @NotEmpty private String serviceAccountKeyFileContent;

  public GcpConfig() {
    super(GCP.name());
  }

  public GcpConfig(String serviceAccountKeyFileContent) {
    this();
    this.serviceAccountKeyFileContent = serviceAccountKeyFileContent;
  }
}
