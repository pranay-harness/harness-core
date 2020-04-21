package software.wings.testutils.encryptionsamples;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.encryption.Encrypted;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.annotation.EncryptableSetting;
import software.wings.settings.SettingValue.SettingVariableTypes;

@Data
@Builder
public class SampleEncryptableSetting implements EncryptableSetting, PersistentEntity, UuidAware {
  public static final String ENCRYPTED_ANNOTATION_VALUE_FIELD = "field1";
  public static final String ENCRYPTED_ANNOTATION_KEY_FIELD = "field2";
  private String uuid;
  private String accountId;
  private SettingVariableTypes type;

  @Encrypted(fieldName = ENCRYPTED_ANNOTATION_VALUE_FIELD, isReference = true) private char[] value;
  @SchemaIgnore @Indexed private String encryptedValue;

  @Encrypted(fieldName = ENCRYPTED_ANNOTATION_KEY_FIELD) private char[] key;
  @SchemaIgnore @Indexed private String encryptedKey;

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  @Override
  public String getAccountId() {
    return accountId;
  }

  @Override
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  @Override
  public SettingVariableTypes getSettingType() {
    return type;
  }
}
