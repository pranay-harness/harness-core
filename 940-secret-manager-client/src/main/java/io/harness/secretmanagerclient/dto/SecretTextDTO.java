/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.secretmanagerclient.dto;

import static io.harness.secretmanagerclient.SecretType.SecretText;

import io.harness.data.validator.SecretTypeAllowedValues;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.ValueType;

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
public class SecretTextDTO extends SecretDTO {
  @SecretTypeAllowedValues(allowedValues = {SecretText}, message = "Invalid value of type")
  @NotNull
  private SecretType type;
  @JsonIgnore private SettingVariableTypes settingVariableType;
  @NotNull private ValueType valueType;
  private String value;
  @JsonIgnore private String path;
  @JsonIgnore private boolean draft;

  @JsonCreator
  @Builder
  public SecretTextDTO(@JsonProperty("account") String account, @JsonProperty("org") String org,
      @JsonProperty("project") String project, @JsonProperty("identifier") String identifier,
      @JsonProperty("secretManager") String secretManager, @JsonProperty("name") String name,
      @JsonProperty("tags") List<String> tags, @JsonProperty("description") String description,
      @JsonProperty("type") SecretType type, @JsonProperty("valueType") ValueType valueType,
      @JsonProperty("value") String value) {
    super(account, org, project, identifier, secretManager, name, tags, description);
    this.type = type;
    this.settingVariableType = SecretType.toSettingVariableType(type);
    this.valueType = valueType;
    this.draft = false;
    if (ValueType.Inline == valueType) {
      this.value = value;
      this.path = null;
    } else if (ValueType.Reference == valueType) {
      this.path = value;
      this.value = null;
    }
  }
}
