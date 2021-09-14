/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.awsconnector;
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

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("AwsCredential")
@JsonDeserialize(using = AwsCredentialDTODeserializer.class)
public class AwsCredentialDTO {
  @Valid CrossAccountAccessDTO crossAccountAccess;
  @NotNull @JsonProperty("type") AwsCredentialType awsCredentialType;
  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @Valid
  AwsCredentialSpecDTO config;

  @Builder
  public AwsCredentialDTO(
      AwsCredentialType awsCredentialType, AwsCredentialSpecDTO config, CrossAccountAccessDTO crossAccountAccess) {
    this.awsCredentialType = awsCredentialType;
    this.config = config;
    this.crossAccountAccess = crossAccountAccess;
  }
}
