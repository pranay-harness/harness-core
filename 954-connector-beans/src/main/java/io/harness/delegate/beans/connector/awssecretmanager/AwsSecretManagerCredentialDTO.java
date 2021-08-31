package io.harness.delegate.beans.connector.awssecretmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("AwsSecretManagerCredential")
@JsonDeserialize(using = AwsSMCredentialDTODeserializer.class)
public class AwsSecretManagerCredentialDTO {
  @NotNull @JsonProperty("type") AwsSecretManagerCredentialType credentialType;
  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @Valid
  AwsSecretManagerCredentialSpecDTO config;

  @Builder
  public AwsSecretManagerCredentialDTO(
      AwsSecretManagerCredentialType credentialType, AwsSecretManagerCredentialSpecDTO config) {
    this.credentialType = credentialType;
    this.config = config;
  }
}
