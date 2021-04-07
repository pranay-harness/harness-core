package io.harness.delegate.beans.connector.awskmsconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialDTODeserializer;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("AwsKmsConnectorCredential")
@JsonDeserialize(using = GcpCredentialDTODeserializer.class)
public class AwsKmsConnectorCredentialDTO {
  @NotNull @JsonProperty("type")
  AwsKmsCredentialType credentialType;
  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @Valid
  AwsKmsCredentialSpecDTO config;

  @Builder
  public AwsKmsConnectorCredentialDTO(AwsKmsCredentialType credentialType, AwsKmsCredentialSpecDTO config) {
    this.credentialType = credentialType;
    this.config = config;
  }
}
