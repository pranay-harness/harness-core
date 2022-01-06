/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.awsconnector;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "AwsCredential", description = "This contains details of the AWS connector credential")
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
