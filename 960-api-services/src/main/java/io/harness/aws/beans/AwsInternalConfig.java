package io.harness.aws.beans;

import io.harness.encryption.Encrypted;

import software.wings.annotation.EncryptableSetting;
import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.settings.SettingVariableTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@JsonTypeName("AWS")
@Data
@Builder
@ToString(exclude = "secretKey")
@EqualsAndHashCode(callSuper = false)
public class AwsInternalConfig implements EncryptableSetting {
  private static final String AWS_URL = "https://aws.amazon.com/";
  @Attributes(title = "Access Key") @Encrypted(fieldName = "access_key", isReference = true) private char[] accessKey;
  @Attributes(title = "Secret Key") @Encrypted(fieldName = "secret_key") private char[] secretKey;
  @Attributes(title = "Use Ec2 Iam role") private boolean useEc2IamCredentials;
  private AwsCrossAccountAttributes crossAccountAttributes;
  private String defaultRegion;
  private boolean assumeCrossAccountRole;
  @JsonIgnore @SchemaIgnore private boolean isCertValidationRequired;

  @Override
  public SettingVariableTypes getSettingType() {
    return null;
  }

  @Override
  public String getAccountId() {
    return null;
  }

  @Override
  public void setAccountId(String accountId) {}
}
