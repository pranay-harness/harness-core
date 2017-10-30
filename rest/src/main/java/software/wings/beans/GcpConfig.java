package software.wings.beans;

import static software.wings.settings.SettingValue.SettingVariableTypes.GCP;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.settings.SettingValue;

/**
 * Created by bzane on 2/28/17
 */
@JsonTypeName("GCP")
@Data
@Builder
public class GcpConfig extends SettingValue implements Encryptable {
  @JsonIgnore @NotEmpty @Encrypted private char[] serviceAccountKeyFileContent;

  @SchemaIgnore @NotEmpty private String accountId;

  @SchemaIgnore private String encryptedServiceAccountKeyFileContent;

  public GcpConfig() {
    super(GCP.name());
  }

  public GcpConfig(
      char[] serviceAccountKeyFileContent, String accountId, String encryptedServiceAccountKeyFileContent) {
    this();
    this.serviceAccountKeyFileContent = serviceAccountKeyFileContent;
    this.accountId = accountId;
    this.encryptedServiceAccountKeyFileContent = encryptedServiceAccountKeyFileContent;
  }
}
