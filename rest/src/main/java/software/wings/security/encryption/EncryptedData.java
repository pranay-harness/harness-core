package software.wings.security.encryption;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.settings.SettingValue.SettingVariableTypes;

/**
 * Created by rsingh on 9/29/17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(value = "encryptedRecords", noClassnameStored = true)
public class EncryptedData extends Base {
  @NotEmpty private String encryptionKey;
  @NotEmpty private char[] encryptedValue;
  @NotEmpty private SettingVariableTypes type;

  @NotEmpty @Indexed private String parentId;

  @NotEmpty @Indexed private String accountId;
}
