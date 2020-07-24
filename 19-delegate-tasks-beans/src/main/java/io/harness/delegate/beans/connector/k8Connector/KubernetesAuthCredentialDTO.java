package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import software.wings.annotation.EncryptableSetting;
import software.wings.settings.SettingVariableTypes;

@Data
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
public abstract class KubernetesAuthCredentialDTO implements EncryptableSetting {
  @ApiModelProperty(hidden = true) @Override public abstract SettingVariableTypes getSettingType();

  @ApiModelProperty(hidden = true) @Override public abstract String getAccountId();

  @ApiModelProperty(hidden = true) @Override public abstract void setAccountId(String accountId);
}
