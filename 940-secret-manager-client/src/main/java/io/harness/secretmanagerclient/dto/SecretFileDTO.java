/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.secretmanagerclient.dto;

import io.harness.data.validator.SecretTypeAllowedValues;
import io.harness.secretmanagerclient.SecretType;

import software.wings.settings.SettingVariableTypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecretFileDTO extends SecretDTO {
  @SecretTypeAllowedValues(allowedValues = {SecretType.SecretFile}, message = "Invalid value of type")
  @NotNull
  private SecretType type;
  @JsonIgnore private SettingVariableTypes settingVariableType;

  @JsonCreator
  @Builder
  public SecretFileDTO(@NotNull @JsonProperty("account") String account, @NotNull @JsonProperty("org") String org,
      @JsonProperty("project") String project, @NotNull @JsonProperty("identifier") String identifier,
      @NotNull @JsonProperty("secretManager") String secretManager, @NotNull @JsonProperty("name") String name,
      @NotNull @JsonProperty("tags") List<String> tags, @JsonProperty("description") String description,
      @NotNull @JsonProperty("type") SecretType type) {
    super(account, org, project, identifier, secretManager, name, tags, description);
    this.type = type;
    this.settingVariableType = SecretType.toSettingVariableType(type);
  }
}
