package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.encryption.Encrypted;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.settings.SettingVariableTypes;

@Data
@Builder
@JsonTypeName("UsernamePassword")
public class KubernetesUserNamePasswordDTO extends KubernetesAuthCredentialDTO {
  @NotBlank String username;

  @Getter(onMethod = @__(@JsonIgnore))
  @JsonIgnore
  @Encrypted(fieldName = "password", isReference = true)
  char[] password;

  @JsonProperty("passwordRef") @NotBlank String encryptedPassword;
  @ApiModelProperty(hidden = true) String accountId;
  // todo @deepak: Remove this field if not required
  private String cacert;

  private static final String DUMMY_ACCOUNT_ID = "AccountId";

  @Override
  public SettingVariableTypes getSettingType() {
    return SettingVariableTypes.KUBERNETES_CLUSTER_NG;
  }
}