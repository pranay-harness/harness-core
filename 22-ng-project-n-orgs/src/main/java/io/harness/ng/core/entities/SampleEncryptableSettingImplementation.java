package io.harness.ng.core.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import software.wings.annotation.EncryptableSetting;
import software.wings.settings.SettingVariableTypes;

@Data
@Builder
@ToString(exclude = "secretText")
public class SampleEncryptableSettingImplementation implements EncryptableSetting {
  @Encrypted(fieldName = "encryptedId") private String secretText;
  @SchemaIgnore private String encryptedSecretText;

  @Override
  @JsonIgnore
  @SchemaIgnore
  public SettingVariableTypes getSettingType() {
    return SettingVariableTypes.SECRET_TEXT;
  }

  @Override
  @JsonIgnore
  @SchemaIgnore
  public String getAccountId() {
    return "kmpySmUISimoRrJL6NL73w";
  }

  @Override
  public void setAccountId(String accountId) {
    // not required
  }
}
