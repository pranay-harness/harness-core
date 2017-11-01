package software.wings.security.encryption;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.security.EncryptionType;
import software.wings.settings.SettingValue.SettingVariableTypes;

/**
 * Created by rsingh on 9/29/17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(value = "encryptedRecords", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EncryptedData extends Base {
  @NotEmpty private String encryptionKey;
  @NotEmpty private char[] encryptedValue;
  @NotEmpty private SettingVariableTypes type;

  @NotEmpty @Indexed private String parentId;

  @NotEmpty @Indexed private String accountId;

  private boolean enabled = true;

  @NotEmpty private String kmsId;

  @NotEmpty private EncryptionType encryptionType;

  @Override
  public String toString() {
    return "EncryptedData{"
        + "type=" + type + ", parentId='" + parentId + '\'' + ", accountId='" + accountId + '\''
        + ", enabled=" + enabled + ", kmsId='" + kmsId + '\'' + ", appId='" + appId + '\'' + "} " + super.toString();
  }
}
