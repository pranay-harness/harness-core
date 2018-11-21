package software.wings.service.impl.security;

import lombok.Builder;
import lombok.Data;
import software.wings.security.EncryptionType;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;

import java.util.Set;

/**
 * @author marklu on 10/18/18
 */
@Data
@Builder
public class ExportableSecret {
  private String uuid;
  private SettingVariableTypes type;
  private String name;
  // If CONFIG_FILE, this field is the base64 encoded file content.
  private String value;
  private EncryptionType encryptionType;
  private Set<String> parentIds;
  private UsageRestrictions usageRestrictions;
}
